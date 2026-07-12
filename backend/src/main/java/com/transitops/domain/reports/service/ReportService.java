package com.transitops.domain.reports.service;

import com.transitops.domain.driver.repository.DriverRepository;
import com.transitops.domain.finance.entity.Expense;
import com.transitops.domain.finance.entity.FuelLog;
import com.transitops.domain.finance.repository.ExpenseRepository;
import com.transitops.domain.finance.repository.FuelLogRepository;
import com.transitops.domain.maintenance.entity.MaintenanceLog;
import com.transitops.domain.maintenance.repository.MaintenanceLogRepository;
import com.transitops.domain.reports.dto.FleetSummary;
import com.transitops.domain.reports.dto.VehicleCostSummary;
import com.transitops.domain.trip.entity.Trip;
import com.transitops.domain.trip.repository.TripRepository;
import com.transitops.domain.vehicle.entity.Vehicle;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final FuelLogRepository fuelLogRepository;
    private final ExpenseRepository expenseRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;

    @Transactional(readOnly = true)
    public FleetSummary getSummary() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<FuelLog> fuelLogs = fuelLogRepository.findAll();
        List<Expense> expenses = expenseRepository.findAll();
        List<MaintenanceLog> maintenanceLogs = maintenanceLogRepository.findAll();

        Map<String, Double> fuelByVehicle = fuelLogs.stream()
                .collect(Collectors.groupingBy(FuelLog::getVehicleId, Collectors.summingDouble(FuelLog::getCost)));

        Map<String, Double> expenseByVehicle = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getVehicleId, Collectors.summingDouble(Expense::getAmount)));

        Map<String, Double> maintenanceByVehicle = maintenanceLogs.stream()
                .collect(Collectors.groupingBy(MaintenanceLog::getVehicleId, Collectors.summingDouble(MaintenanceLog::getCost)));

        List<VehicleCostSummary> costByVehicle = vehicles.stream()
                .map(v -> {
                    double fuel = fuelByVehicle.getOrDefault(v.getId(), 0.0);
                    double expense = expenseByVehicle.getOrDefault(v.getId(), 0.0);
                    double maintenance = maintenanceByVehicle.getOrDefault(v.getId(), 0.0);
                    return new VehicleCostSummary(
                            v.getId(), v.getRegNumber(), fuel, expense, maintenance, fuel + expense + maintenance
                    );
                })
                .toList();

        double totalFuelCost = fuelLogs.stream().mapToDouble(FuelLog::getCost).sum();
        double totalExpenseCost = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double totalMaintenanceCost = maintenanceLogs.stream().mapToDouble(MaintenanceLog::getCost).sum();

        List<Trip> trips = tripRepository.findAll();

        return new FleetSummary(
                vehicles.size(),
                countByEnum(vehicles.stream().map(v -> v.getStatus().name())),
                driverRepository.count(),
                countByEnum(driverRepository.findAll().stream().map(d -> d.getStatus().name())),
                trips.size(),
                countByEnum(trips.stream().map(t -> t.getStatus().name())),
                totalFuelCost,
                totalExpenseCost,
                totalMaintenanceCost,
                totalFuelCost + totalExpenseCost + totalMaintenanceCost,
                costByVehicle
        );
    }

    private Map<String, Long> countByEnum(java.util.stream.Stream<String> stream) {
        return stream.collect(Collectors.groupingBy(s -> s, LinkedHashMap::new, Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        FleetSummary summary = getSummary();
        StringBuilder csv = new StringBuilder();

        csv.append("vehicleId,regNumber,totalFuelCost,totalExpenseCost,totalMaintenanceCost,totalCost\n");
        for (VehicleCostSummary v : summary.costByVehicle()) {
            csv.append(escape(v.vehicleId())).append(',')
                    .append(escape(v.regNumber())).append(',')
                    .append(v.totalFuelCost()).append(',')
                    .append(v.totalExpenseCost()).append(',')
                    .append(v.totalMaintenanceCost()).append(',')
                    .append(v.totalCost()).append('\n');
        }

        csv.append("\nFLEET TOTALS\n");
        csv.append("totalVehicles,").append(summary.totalVehicles()).append('\n');
        csv.append("totalDrivers,").append(summary.totalDrivers()).append('\n');
        csv.append("totalTrips,").append(summary.totalTrips()).append('\n');
        csv.append("totalFuelCost,").append(summary.totalFuelCost()).append('\n');
        csv.append("totalExpenseCost,").append(summary.totalExpenseCost()).append('\n');
        csv.append("totalMaintenanceCost,").append(summary.totalMaintenanceCost()).append('\n');
        csv.append("totalFleetCost,").append(summary.totalFleetCost()).append('\n');

        return csv.toString();
    }

    private String escape(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}