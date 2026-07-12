package com.transitops.domain.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record MaintenanceCreateRequest(
        @NotBlank String vehicleId,
        @NotBlank String type,
        @PositiveOrZero double cost
) {}