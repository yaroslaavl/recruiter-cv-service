package org.yaroslaavl.cvservice.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import org.yaroslaavl.cvservice.validation.CVUpload;

public record CVUploadDto(
        @CVUpload @NotNull MultipartFile cv
) { }
