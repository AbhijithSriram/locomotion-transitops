package com.transitops.domain.reports.dto;

public record VehicleCostSummary(
        String vehicleId,
        String regNumber,
        double totalFuelCost,
        double totalExpenseCost,
        double totalMaintenanceCost,
        double totalCost
) {}