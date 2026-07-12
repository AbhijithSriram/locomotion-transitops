package com.transitops.domain.driver.dto;

import com.transitops.common.enums.DriverStatus;

import java.time.LocalDate;

public record DriverResponse(
        String id,
        String name,
        String licenseNumber,
        String licenseCategory,
        String contact,
        LocalDate licenseExpiry,
        double safetyScore,
        DriverStatus status
) {}