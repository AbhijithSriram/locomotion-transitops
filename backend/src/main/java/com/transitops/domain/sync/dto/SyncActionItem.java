package com.transitops.domain.sync.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SyncActionItem(
        @NotBlank String idempotencyKey,
        @NotBlank String type,
        @NotBlank String driverId,
        @NotNull JsonNode payload
) {}