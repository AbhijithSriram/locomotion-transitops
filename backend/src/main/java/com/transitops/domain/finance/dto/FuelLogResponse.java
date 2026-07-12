package com.transitops.domain.finance.dto;

import java.time.Instant;

public record FuelLogResponse(
        String id,
        String vehicleId,
        String tripId,
        double liters,
        double cost,
        double odometer,
        Instant date,
        Instant loggedAt
) {}