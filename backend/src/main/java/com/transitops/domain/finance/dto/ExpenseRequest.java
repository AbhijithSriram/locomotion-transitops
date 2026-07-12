package com.transitops.domain.finance.dto;

import com.transitops.common.enums.ExpenseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ExpenseRequest(
        @NotBlank String vehicleId,
        String tripId,
        @NotNull ExpenseCategory category,
        @PositiveOrZero double amount,
        String description
) {}