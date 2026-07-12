package com.transitops.domain.vehicle.dto;

import com.transitops.common.enums.VehicleStatus;

import java.time.Instant;

public record VehicleResponse(
        String id,
        String regNumber,
        String name,
        String type,
        double maxLoadKg,
        double odometer,
        double acquisitionCost,
        VehicleStatus status,
        String transportMode,
        double tyres,
        double engine,
        double brakes,
        double riskScore,
        Instant lastServiceAt
) {}