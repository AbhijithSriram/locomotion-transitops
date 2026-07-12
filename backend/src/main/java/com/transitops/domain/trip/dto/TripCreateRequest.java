package com.transitops.domain.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TripCreateRequest(
        @NotBlank String source,
        @NotBlank String destination,
        @NotBlank String vehicleId,
        @NotBlank String driverId,
        @Positive double cargoWeightKg,
        @Positive double plannedDistanceKm,
        String routePolyline
) {}