package com.transitops.domain.trip.dto;

import com.transitops.common.enums.TripStatus;

import java.time.Instant;

public record TripResponse(
        String id,
        String source,
        String destination,
        String vehicleId,
        String driverId,
        double cargoWeightKg,
        double plannedDistanceKm,
        Double actualDistanceKm,
        TripStatus status,
        String routePolyline,
        Instant createdAt,
        Instant dispatchedAt,
        Instant completedAt
) {}