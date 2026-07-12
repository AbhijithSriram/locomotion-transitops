package com.transitops.domain.seed;

import com.transitops.common.enums.DriverStatus;
import com.transitops.common.enums.ExpenseCategory;
import com.transitops.common.enums.TripStatus;
import com.transitops.common.enums.VehicleStatus;
import com.transitops.common.util.Ids;
import com.transitops.domain.driver.entity.Driver;
import com.transitops.domain.driver.repository.DriverRepository;
import com.transitops.domain.finance.entity.Expense;
import com.transitops.domain.finance.entity.FuelLog;
import com.transitops.domain.finance.repository.ExpenseRepository;
import com.transitops.domain.finance.repository.FuelLogRepository;
import com.transitops.domain.maintenance.entity.MaintenanceLog;
import com.transitops.domain.maintenance.repository.MaintenanceLogRepository;
import com.transitops.domain.trip.entity.Trip;
import com.transitops.domain.trip.repository.TripRepository;
import com.transitops.domain.vehicle.entity.Vehicle;
import com.transitops.domain.vehicle.entity.VehicleHealth;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

/**
 * Seeds a realistic demo fleet: vehicles in every status, drivers with a couple
 * deliberately invalid (expired license / suspended) for testing DispatchService
 * rejections, historical trips with fuel/expense trails, and a couple of
 * in-flight trips so the map/dashboard has something moving on first load.
 *
 * Gated by transitops.seed.enabled — set false in application.yml for a clean DB.
 * Also skips automatically if vehicles already exist, so re-running the app
 * doesn't duplicate data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // after UserSeeder (admin account), which should run first
public class DemoDataSeeder implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final FuelLogRepository fuelLogRepository;
    private final ExpenseRepository expenseRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;

    @Value("${transitops.seed.enabled:false}")
    private boolean seedEnabled;

    private final Random random = new Random(42); // fixed seed → reproducible demo data

    // Ahmedabad/Gandhinagar-area depot coordinates, matches B's simulation zone
    private static final String[] DEPOT_NAMES = {
            "Gandhinagar Depot", "Ahmedabad Hub", "Vatva Industrial Area",
            "Sanand Warehouse", "Kalol Depot", "Mansa Yard"
    };
    private static final double[][] DEPOT_COORDS = {
            {23.2156, 72.6369}, {23.0225, 72.5714}, {22.9676, 72.6469},
            {22.9922, 72.3822}, {23.2422, 72.4946}, {23.4256, 72.6567}
    };

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Demo data seeding disabled (transitops.seed.enabled=false) — skipping.");
            return;
        }
        if (vehicleRepository.count() > 0) {
            log.info("Vehicles already exist — skipping demo seed to avoid duplicates.");
            return;
        }

        log.info("Seeding demo fleet data...");

        List<Vehicle> vehicles = seedVehicles();
        List<Driver> drivers = seedDrivers();
        seedHistoricalTrips(vehicles, drivers);
        seedInFlightTrips(vehicles, drivers);

        log.info("Demo data seeding complete: {} vehicles, {} drivers.", vehicles.size(), drivers.size());
    }

    private List<Vehicle> seedVehicles() {
        record VSpec(String reg, String name, String type, double maxLoad, double cost, VehicleStatus status, double odo) {}

        List<VSpec> specs = List.of(
                new VSpec("GJ01AB1001", "Truck Alpha", "TRUCK", 8000, 1800000, VehicleStatus.AVAILABLE, 42350),
                new VSpec("GJ01AB1002", "Truck Bravo", "TRUCK", 8000, 1800000, VehicleStatus.AVAILABLE, 51200),
                new VSpec("GJ01AB1003", "Truck Charlie", "TRUCK", 6000, 1500000, VehicleStatus.ON_TRIP, 33780),
                new VSpec("GJ01AB1004", "Truck Delta", "TRUCK", 6000, 1500000, VehicleStatus.ON_TRIP, 28900),
                new VSpec("GJ01AB1005", "Van Echo", "VAN", 2000, 900000, VehicleStatus.AVAILABLE, 19500),
                new VSpec("GJ01AB1006", "Van Foxtrot", "VAN", 2000, 900000, VehicleStatus.IN_SHOP, 61200),
                new VSpec("GJ01AB1007", "Truck Golf", "TRUCK", 10000, 2100000, VehicleStatus.RETIRED, 128400),
                new VSpec("GJ01AB1008", "Truck Hotel", "TRUCK", 8000, 1850000, VehicleStatus.BROKEN_DOWN, 45670)
        );

        return specs.stream().map(s -> {
            String id = Ids.newId();
            VehicleHealth health = VehicleHealth.builder()
                    .vehicleId(id)
                    .tyres(s.status() == VehicleStatus.BROKEN_DOWN ? 22 : 60 + random.nextInt(40))
                    .engine(s.status() == VehicleStatus.BROKEN_DOWN ? 15 : 60 + random.nextInt(40))
                    .brakes(s.status() == VehicleStatus.BROKEN_DOWN ? 30 : 60 + random.nextInt(40))
                    .riskScore(s.status() == VehicleStatus.BROKEN_DOWN ? 91 : random.nextInt(30))
                    .lastServiceAt(Instant.now().minus(10 + random.nextInt(80), ChronoUnit.DAYS))
                    .build();

            Vehicle v = Vehicle.builder()
                    .id(id)
                    .regNumber(s.reg())
                    .name(s.name())
                    .type(s.type())
                    .maxLoadKg(s.maxLoad())
                    .odometer(s.odo())
                    .acquisitionCost(s.cost())
                    .status(s.status())
                    .health(health)
                    .transportMode("TRUCK")
                    .build();

            return vehicleRepository.save(v);
        }).toList();
    }

    private List<Driver> seedDrivers() {
        record DSpec(String name, String license, String category, String contact, LocalDate expiry, DriverStatus status, double safety) {}

        List<DSpec> specs = List.of(
                new DSpec("Ravi Kumar", "GJ0420230001", "HMV", "9876500001", LocalDate.now().plusYears(2), DriverStatus.AVAILABLE, 95),
                new DSpec("Suresh Patel", "GJ0420230002", "HMV", "9876500002", LocalDate.now().plusYears(1), DriverStatus.AVAILABLE, 88),
                new DSpec("Anil Sharma", "GJ0420230003", "HMV", "9876500003", LocalDate.now().plusMonths(6), DriverStatus.ON_TRIP, 91),
                new DSpec("Vikram Singh", "GJ0420230004", "LMV", "9876500004", LocalDate.now().plusMonths(9), DriverStatus.ON_TRIP, 78),
                new DSpec("Manoj Yadav", "GJ0420230005", "HMV", "9876500005", LocalDate.now().plusYears(3), DriverStatus.AVAILABLE, 99),
                new DSpec("Deepak Joshi", "GJ0420230006", "LMV", "9876500006", LocalDate.now().plusMonths(4), DriverStatus.OFF_DUTY, 82),
                // deliberately invalid — exercises DispatchService rejection rules
                new DSpec("Rajesh Mehta", "GJ0420230007", "HMV", "9876500007", LocalDate.now().minusMonths(2), DriverStatus.AVAILABLE, 60),
                new DSpec("Kiran Desai", "GJ0420230008", "HMV", "9876500008", LocalDate.now().plusYears(1), DriverStatus.SUSPENDED, 40)
        );

        return specs.stream().map(s -> driverRepository.save(
                Driver.builder()
                        .id(Ids.newId())
                        .name(s.name())
                        .licenseNumber(s.license())
                        .licenseCategory(s.category())
                        .contact(s.contact())
                        .licenseExpiry(s.expiry())
                        .safetyScore(s.safety())
                        .status(s.status())
                        .build()
        )).toList();
    }

    /** Completed trips in the past, each with a fuel log + a couple of expenses, for report/cost-rollup demo. */
    private void seedHistoricalTrips(List<Vehicle> vehicles, List<Driver> drivers) {
        for (int i = 0; i < 6; i++) {
            int fromIdx = random.nextInt(DEPOT_NAMES.length);
            int toIdx;
            do { toIdx = random.nextInt(DEPOT_NAMES.length); } while (toIdx == fromIdx);

            Vehicle v = vehicles.get(random.nextInt(vehicles.size()));
            Driver d = drivers.get(random.nextInt(drivers.size()));

            Instant createdAt = Instant.now().minus(5 + random.nextInt(20), ChronoUnit.DAYS);
            Instant dispatchedAt = createdAt.plus(1, ChronoUnit.HOURS);
            Instant completedAt = dispatchedAt.plus(3 + random.nextInt(4), ChronoUnit.HOURS);

            double plannedDistance = 30 + random.nextInt(120);

            Trip trip = Trip.builder()
                    .id(Ids.newId())
                    .source(DEPOT_NAMES[fromIdx])
                    .destination(DEPOT_NAMES[toIdx])
                    .sourceName(DEPOT_NAMES[fromIdx])
                    .sourceLat(DEPOT_COORDS[fromIdx][0])
                    .sourceLng(DEPOT_COORDS[fromIdx][1])
                    .destinationName(DEPOT_NAMES[toIdx])
                    .destinationLat(DEPOT_COORDS[toIdx][0])
                    .destinationLng(DEPOT_COORDS[toIdx][1])
                    .vehicleId(v.getId())
                    .driverId(d.getId())
                    .cargoWeightKg(Math.min(v.getMaxLoadKg() * 0.6, 500 + random.nextInt(1500)))
                    .plannedDistanceKm(plannedDistance)
                    .actualDistanceKm(plannedDistance + random.nextInt(10))
                    .status(TripStatus.COMPLETED)
                    .createdAt(createdAt)
                    .dispatchedAt(dispatchedAt)
                    .completedAt(completedAt)
                    .build();
            tripRepository.save(trip);

            double liters = 25 + random.nextDouble() * 30;
            fuelLogRepository.save(FuelLog.builder()
                    .id(Ids.newId())
                    .vehicleId(v.getId())
                    .tripId(trip.getId())
                    .liters(Math.round(liters * 10.0) / 10.0)
                    .cost(Math.round(liters * 95.0))
                    .odometer(v.getOdometer())
                    .date(completedAt)
                    .loggedAt(completedAt)
                    .build());

            if (random.nextBoolean()) {
                expenseRepository.save(Expense.builder()
                        .id(Ids.newId())
                        .vehicleId(v.getId())
                        .tripId(trip.getId())
                        .category(ExpenseCategory.TOLL)
                        .amount(80 + random.nextInt(150))
                        .description("Highway toll")
                        .incurredAt(completedAt)
                        .build());
            }
        }

        // one maintenance record for variety, closed
        Vehicle serviced = vehicles.get(0);
        MaintenanceLog log = MaintenanceLog.builder()
                .id(Ids.newId())
                .vehicleId(serviced.getId())
                .type("Routine Service")
                .status("closed")
                .cost(4500)
                .createdAt(Instant.now().minus(15, ChronoUnit.DAYS))
                .closedAt(Instant.now().minus(14, ChronoUnit.DAYS))
                .build();
        maintenanceLogRepository.save(log);
    }

    /**
     * Trips that are DISPATCHED right now, matching the vehicles already seeded as
     * ON_TRIP, so B's SimulationService.init() picks them up on startup and the
     * dashboard map has movement immediately.
     */
    private void seedInFlightTrips(List<Vehicle> vehicles, List<Driver> drivers) {
        List<Vehicle> onTripVehicles = vehicles.stream()
                .filter(v -> v.getStatus() == VehicleStatus.ON_TRIP)
                .toList();
        List<Driver> onTripDrivers = drivers.stream()
                .filter(d -> d.getStatus() == DriverStatus.ON_TRIP)
                .toList();

        int count = Math.min(onTripVehicles.size(), onTripDrivers.size());

        for (int i = 0; i < count; i++) {
            int fromIdx = random.nextInt(DEPOT_NAMES.length);
            int toIdx;
            do { toIdx = random.nextInt(DEPOT_NAMES.length); } while (toIdx == fromIdx);

            Vehicle v = onTripVehicles.get(i);
            Driver d = onTripDrivers.get(i);
            double plannedDistance = 40 + random.nextInt(100);

            Trip trip = Trip.builder()
                    .id(Ids.newId())
                    .source(DEPOT_NAMES[fromIdx])
                    .destination(DEPOT_NAMES[toIdx])
                    .sourceName(DEPOT_NAMES[fromIdx])
                    .sourceLat(DEPOT_COORDS[fromIdx][0])
                    .sourceLng(DEPOT_COORDS[fromIdx][1])
                    .destinationName(DEPOT_NAMES[toIdx])
                    .destinationLat(DEPOT_COORDS[toIdx][0])
                    .destinationLng(DEPOT_COORDS[toIdx][1])
                    .vehicleId(v.getId())
                    .driverId(d.getId())
                    .cargoWeightKg(Math.min(v.getMaxLoadKg() * 0.5, 800))
                    .plannedDistanceKm(plannedDistance)
                    .status(TripStatus.DISPATCHED)
                    .createdAt(Instant.now().minus(20, ChronoUnit.MINUTES))
                    .dispatchedAt(Instant.now().minus(15, ChronoUnit.MINUTES))
                    .build();

            tripRepository.save(trip);
        }
    }
}