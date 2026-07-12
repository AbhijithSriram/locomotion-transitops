package com.transitops.domain.maintenance.dto;

import java.time.Instant;

public record MaintenanceResponse(
        String id,
        String vehicleId,
        String type,
        String status,
        double cost,
        Instant createdAt,
        Instant closedAt
) {}