package org.yaroslaavl.cvservice.service.impl;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaroslaavl.cvservice.database.entity.UserCV;
import org.yaroslaavl.cvservice.database.repository.UserCVRepository;
import org.yaroslaavl.cvservice.dto.CVSummaryDto;
import org.yaroslaavl.cvservice.dto.CVUploadDto;
import org.yaroslaavl.cvservice.exception.*;
import org.yaroslaavl.cvservice.feignClient.user.UserFeignClient;
import org.yaroslaavl.cvservice.mapper.UserCVMapper;
import org.yaroslaavl.cvservice.service.MinioCVService;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioCVServiceImpl implements MinioCVService {

    @Value("${cv.max_elements}")
    private Integer maxElements;

    @Value("${minio.bucket-name}")
    private String bucket;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${bucket.folder}")
    private String folder;

    @Value("${feign.token-type}")
    private String tokenType;

    private final MinioClient minioClient;
    private final UserCVMapper userCVMapper;
    private final UserCVRepository userCVRepository;
    private final UserFeignClient userFeignClient;

    private static final String SUB = "sub";
    private static final String EXTENSION = ".pdf";

    @Override
    @Transactional
    public void upload(CVUploadDto cvUploadDto) {
        checkUserAccountStatus();

        try {
            String cvLink = uploadMinioCv(cvUploadDto.cv(), cvUploadDto.isMain());

            UserCV userCV = UserCV.builder()
                    .isMain(cvUploadDto.isMain())
                    .userId(getAuthenticatedUserSubOrToken())
                    .filePath(cvLink)
                    .build();

            userCVRepository.save(userCV);
        } catch (Exception e) {
            log.error("Unexpected error during cv upload for user", e);
            throw new CVUploadException("Unexpected error during cv upload");
        }
    }

    @Override
    @Transactional
    public void remove(boolean isMain) {
        checkUserAccountStatus();

        String userId = getAuthenticatedUserSubOrToken();
        UserCV userCV = userCVRepository.findByIsMainAndUserId(isMain, userId)
                .orElseThrow(() -> new EntityNotFoundException("CV not found"));

        if (!Objects.equals(userCV.getUserId(), userId)) {
            throw new UserHasNoPermissionException("User has no permission to delete this cv");
        }

        String minioCV = getMinioCV(userId, isMain);
        if (minioCV != null && !minioCV.isEmpty()) {
            removeObject(minioCV);
            log.info("Removed CV from MinIO: {}", minioCV);
        } else {
            log.warn("CV file not found in MinIO: {}", minioCV);
        }

        userCVRepository.delete(userCV);
        log.info("Deleted CV record from DB for user {} (isMain={})", userId, isMain);
    }

    @Override
    @SneakyThrows
    public String getCvForCandidate(UUID cvId, boolean isMain) {
        userCVRepository.findById(cvId)
                        .orElseThrow(() -> new EntityNotFoundException("CV not found"));

        checkUserAccountStatus();

        String userId = getAuthenticatedUserSubOrToken();
        String minioCV = getMinioCV(userId, isMain);

        UserCV userCV = userCVRepository.findByFilePath(minioUrl + bucket + "/" + minioCV)
                .orElseThrow(() -> new EntityNotFoundException("CV not found"));

        if (!Objects.equals(userCV.getUserId(), userId)) {
            throw new UserHasNoPermissionException("User has no permission to read this cv");
        }

        return generatePresignedUrl(minioCV);
    }

    @Override
    public String getCvForRecruiter(UUID cvId) {
        UserCV userCV = userCVRepository.findById(cvId)
                .orElseThrow(() -> new EntityNotFoundException("CV not found"));

        String minioCV = getMinioCV(userCV.getUserId(), userCV.getIsMain());
        return generatePresignedUrl(minioCV);
    }

    @Override
    public List<CVSummaryDto> findAllCandidateCvs() {
        List<UserCV> allUserCvs = userCVRepository.findAllByUserId(getAuthenticatedUserSubOrToken());
        return userCVMapper.toSummaryDto(allUserCvs);
    }

    private String getMinioCV(String userId, boolean isMain) {
        String formattedFolder = MessageFormat.format(folder, userId);
        return objectExist(formattedFolder, isMain);
    }

    @SneakyThrows
    private String uploadMinioCv(MultipartFile cvUpload, Boolean isMain) {
        if (isMaxElementsReached(getAuthenticatedUserSubOrToken())) {
            throw new OutOfQuantityException("Max elements reached");
        }

        try {
            boolean isPresent = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!isPresent) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }

            return storeCvInMinio(cvUpload, isMain, getAuthenticatedUserSubOrToken());
        } catch (MinioException me) {
            log.warn("Error occurred: {}", String.valueOf(me));
            log.warn("HTTP trace: {}", me.httpTrace());
            throw new FileStorageException("Could not store file in MinIO");
        }
    }

    @SneakyThrows
    private void putObject(String bucket, String objectName, boolean isMain, MultipartFile file) {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName + (isMain ? "main" : "notMain") + EXTENSION)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
    }

    @SneakyThrows
    private void removeObject(String file) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(file)
                    .build());
        } catch (MinioException e) {
            log.warn("Failed to remove object from storage", e);
            throw new FileStorageException("Failed to remove");
        }
    }

    private String storeCvInMinio(MultipartFile file, boolean isMain, String userId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String formattedFolder = MessageFormat.format(folder, userId);
        String cvPath = objectExist(formattedFolder, isMain);

        if ((cvPath != null && !cvPath.isEmpty())) {
            removeObject(cvPath);
        }

        putObject(bucket, formattedFolder, isMain, file);

        return minioUrl + bucket + "/" + formattedFolder + (isMain ? "main" : "notMain") + EXTENSION;
    }

    private String objectExist(String formattedFolder, boolean isMain) {
        try {
            Iterable<Result<Item>> results =
                    minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(formattedFolder + (isMain ? "main" : "notMain")).build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.objectName().startsWith(formattedFolder + (isMain ? "main" : "notMain"))) {
                    return item.objectName();
                }
            }

            return null;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return null;
            } else {
                throw new RuntimeException("Error occurred while checking object existence", e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean isMaxElementsReached(String userId) {
        long elements = userCVRepository.countByUserId(userId);
        log.info("Elements: {}", elements);

        return maxElements == elements;
    }

    private String getAuthenticatedUserSubOrToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return jwt.getTokenAttributes().get(SUB).toString();
        }

        log.warn("Authentication is not JwtAuthenticationToken or it has no sub");
        return null;
    }

    private void checkUserAccountStatus() {
        boolean isExistsAndApproved = userFeignClient.isApproved(getAuthenticatedUserSubOrToken());

        if (!isExistsAndApproved) {
            throw new CVUploadException("User is not approved or not exists");
        }
    }

    @SneakyThrows
    private String generatePresignedUrl(String minioCV) {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .method(Method.GET)
                .object(minioCV)
                .expiry(30, TimeUnit.MINUTES)
                .build());
    }
}
