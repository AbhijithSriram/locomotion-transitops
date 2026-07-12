package com.transitops.domain.driver.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DriverRequest(
        @NotBlank String name,
        @NotBlank String licenseNumber,
        @NotBlank String licenseCategory,
        String contact,
        @NotNull @Future LocalDate licenseExpiry
) {}