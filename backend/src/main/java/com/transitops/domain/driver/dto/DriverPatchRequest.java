package com.transitops.domain.driver.dto;

import com.transitops.common.enums.DriverStatus;

import java.time.LocalDate;

public record DriverPatchRequest(
        String name,
        String contact,
        String licenseCategory,
        LocalDate licenseExpiry,
        Double safetyScore,
        DriverStatus status
) {}