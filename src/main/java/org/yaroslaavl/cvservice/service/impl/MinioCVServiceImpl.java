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
import org.yaroslaavl.cvservice.database.repository.CVRepository;
import org.yaroslaavl.cvservice.dto.CVSummaryDto;
import org.yaroslaavl.cvservice.dto.CVUploadDto;
import org.yaroslaavl.cvservice.exception.*;
import org.yaroslaavl.cvservice.feignClient.user.UserFeignClient;
import org.yaroslaavl.cvservice.mapper.CVMapper;
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

    private final MinioClient minioClient;
    private final CVMapper CVMapper;
    private final CVRepository CVRepository;
    private final UserFeignClient userFeignClient;

    private static final String SUB = "sub";
    private static final String EXTENSION = ".pdf";

    /**
     * Uploads the user's CV to the storage and saves the corresponding information in the database.
     *
     * @param cvUploadDto Object containing CV file and associated metadata such as whether it is the main CV.
     *                    The CV file is uploaded and a link is generated, which is stored along with the user's ID.
     *                    The user must be authenticated and have an active account to perform this operation.
     * @throws CVUploadException if an unexpected error occurs during the CV upload process.
     */
    @Override
    @Transactional
    public void upload(CVUploadDto cvUploadDto) {
        checkUserAccountStatus();

        try {
            if (Objects.requireNonNull(cvUploadDto.cv().getOriginalFilename()).length() >= 100) {
                throw new CVUploadException("File name is too long");
            }

            String cvLink = uploadMinioCv(cvUploadDto.cv(), cvUploadDto.isMain());

            UserCV userCV = UserCV.builder()
                    .isMain(cvUploadDto.isMain())
                    .fileName(cvUploadDto.cv().getOriginalFilename().replace(EXTENSION, ""))
                    .userId(getAuthenticatedUserSubOrToken())
                    .filePath(cvLink)
                    .build();

            CVRepository.save(userCV);
        } catch (Exception e) {
            log.error("Unexpected error during cv upload for user", e);
            throw new CVUploadException("Unexpected error during cv upload");
        }
    }

    /**
     * Removes the CV associated with the authenticated user.
     * This method verifies user account status, retrieves the CV by the specified
     * `isMain` parameter, validates user permissions, deletes the associated file
     * from storage (if it exists), and removes the CV record from the database.
     *
     * @param isMain Flag indicating whether the CV to be removed is the main CV
     *               for the authenticated user.
     * @throws EntityNotFoundException If the CV associated with the specified
     *                                 `isMain` parameter and user ID is not found.
     * @throws UserHasNoPermissionException If the authenticated user is not
     *                                       permitted to delete the CV.
     */
    @Override
    @Transactional
    public void remove(boolean isMain) {
        checkUserAccountStatus();

        String userId = getAuthenticatedUserSubOrToken();
        UserCV userCV = CVRepository.findByIsMainAndUserId(isMain, userId)
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

        CVRepository.delete(userCV);
        log.info("Deleted CV record from DB for user {} (isMain={})", userId, isMain);
    }

    /**
     * Retrieves the CV file URL for a specified candidate.
     *
     * @param cvId The UUID of the CV to be retrieved.
     * @param isMain A boolean flag indicating if the main CV should be retrieved.
     * @return A presigned URL to access the requested CV file.
     * @throws EntityNotFoundException If the specified CV is not found in the repository.
     * @throws UserHasNoPermissionException If the authenticated user does not have permission to access the CV.
     */
    @Override
    @SneakyThrows
    public String getCvForCandidate(UUID cvId, boolean isMain) {
        CVRepository.findById(cvId)
                        .orElseThrow(() -> new EntityNotFoundException("CV not found"));

        checkUserAccountStatus();

        String userId = getAuthenticatedUserSubOrToken();
        String minioCV = getMinioCV(userId, isMain);

        UserCV userCV = CVRepository.findByFilePath(minioUrl + bucket + "/" + minioCV)
                .orElseThrow(() -> new EntityNotFoundException("CV not found"));

        if (!Objects.equals(userCV.getUserId(), userId)) {
            throw new UserHasNoPermissionException("User has no permission to response this cv");
        }

        return generatePresignedUrl(minioCV);
    }

    /**
     * Retrieves a CV for a recruiter by its unique identifier.
     *
     * @param cvId the unique identifier of the CV to be retrieved.
     * @return a presigned URL for accessing the CV.
     * @throws EntityNotFoundException if the CV with the specified identifier is not found.
     */
    @Override
    public String getCvForRecruiter(UUID cvId) {
        UserCV userCV = CVRepository.findById(cvId)
                .orElseThrow(() -> new EntityNotFoundException("CV not found"));

        String minioCV = getMinioCV(userCV.getUserId(), userCV.getIsMain());
        return generatePresignedUrl(minioCV);
    }

    /**
     * Retrieves all CV summaries for the authenticated candidate.
     * Fetches all CVs associated with the currently logged-in user and converts them to a list of CV summary DTOs.
     *
     * @return a list of CVSummaryDto objects representing the summaries of all CVs associated with the authenticated user
     */
    @Override
    public List<CVSummaryDto> findAllCandidateCvs() {
        List<UserCV> allUserCvs = CVRepository.findAllByUserId(getAuthenticatedUserSubOrToken());
        return CVMapper.toSummaryDto(allUserCvs);
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
        long elements = CVRepository.countByUserId(userId);
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
