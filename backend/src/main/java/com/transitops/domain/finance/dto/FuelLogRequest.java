package com.transitops.domain.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record FuelLogRequest(
        @NotBlank String vehicleId,
        String tripId,
        @Positive double liters,
        @PositiveOrZero double cost,
        Double odometer
) {}