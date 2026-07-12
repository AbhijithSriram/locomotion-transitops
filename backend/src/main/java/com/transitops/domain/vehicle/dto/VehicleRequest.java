package com.transitops.domain.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record VehicleRequest(
        @NotBlank String regNumber,
        @NotBlank String name,
        @NotBlank String type,
        @Positive double maxLoadKg,
        double acquisitionCost,
        String transportMode
) {}