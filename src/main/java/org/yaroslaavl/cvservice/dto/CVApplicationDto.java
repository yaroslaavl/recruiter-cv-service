package org.yaroslaavl.cvservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CVApplicationDto(
        @NotNull UUID id,
        @NotBlank String presignedUrl,
        @NotBlank String fileName
) { }
