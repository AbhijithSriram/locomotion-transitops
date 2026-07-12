package com.transitops.domain.finance.dto;

import com.transitops.common.enums.ExpenseCategory;

import java.time.Instant;

public record ExpenseResponse(
        String id,
        String vehicleId,
        String tripId,
        ExpenseCategory category,
        double amount,
        String description,
        Instant incurredAt
) {}