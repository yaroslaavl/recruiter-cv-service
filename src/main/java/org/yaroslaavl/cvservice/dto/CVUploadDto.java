package org.yaroslaavl.cvservice.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record CVUploadDto(
        @NotNull MultipartFile cv,
        @NotNull Boolean isMain
) { }
