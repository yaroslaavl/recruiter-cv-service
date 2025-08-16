package org.yaroslaavl.cvservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CVSummaryDto(
        @NotNull UUID cvId,
        @NotNull Boolean isMain,
        @NotNull LocalDateTime uploadedAt
) { }
