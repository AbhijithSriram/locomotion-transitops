package com.transitops.domain.vehicle.dto;

import com.transitops.common.enums.VehicleStatus;

public record VehiclePatchRequest(
        String name,
        String type,
        Double maxLoadKg,
        VehicleStatus status,
        Double odometer
) {}