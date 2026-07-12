package com.transitops.domain.reports.dto;

import java.util.List;
import java.util.Map;

public record FleetSummary(
        long totalVehicles,
        Map<String, Long> vehiclesByStatus,
        long totalDrivers,
        Map<String, Long> driversByStatus,
        long totalTrips,
        Map<String, Long> tripsByStatus,
        double totalFuelCost,
        double totalExpenseCost,
        double totalMaintenanceCost,
        double totalFleetCost,
        List<VehicleCostSummary> costByVehicle
) {}