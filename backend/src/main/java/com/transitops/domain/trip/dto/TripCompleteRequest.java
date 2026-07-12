package com.transitops.domain.trip.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record TripCompleteRequest(
        @PositiveOrZero double finalOdometer,
        Double actualDistanceKm
) {}