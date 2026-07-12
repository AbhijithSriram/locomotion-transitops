package com.transitops.simulation.service;

import com.transitops.common.enums.*;
import com.transitops.domain.driver.entity.Driver;
import com.transitops.domain.driver.repository.DriverRepository;
import com.transitops.domain.finance.entity.Expense;
import com.transitops.domain.finance.entity.FuelLog;
import com.transitops.domain.finance.repository.ExpenseRepository;
import com.transitops.domain.finance.repository.FuelLogRepository;
import com.transitops.domain.trip.entity.Trip;
import com.transitops.domain.trip.repository.TripRepository;
import com.transitops.domain.vehicle.entity.Vehicle;
import com.transitops.domain.vehicle.entity.VehicleHealth;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class SimulationService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final FuelLogRepository fuelLogRepository;
    private final ExpenseRepository expenseRepository;
    private final RouteService routeService;
    private final SimpMessagingTemplate messagingTemplate;

    private int speedMultiplier = 1;
    private final Map<String, SimulatedTrip> activeTrips = new ConcurrentHashMap<>();
    private final ScheduledExecutorService rescueScheduler = Executors.newScheduledThreadPool(1);

    private static final Map<String, double[]> DEPOT_COORDS = Map.of(
        "Gandhinagar Depot", new double[]{23.2156, 72.6369},
        "Ahmedabad Hub", new double[]{23.0225, 72.5714},
        "Vatva Industrial Area", new double[]{22.9676, 72.6469},
        "Sanand Warehouse", new double[]{22.9922, 72.3822},
        "Kalol Depot", new double[]{23.2422, 72.4946},
        "Mansa Yard", new double[]{23.4256, 72.6567},
        "Chennai Depot", new double[]{13.0827, 80.2707},
        "Bangalore Hub", new double[]{12.9716, 77.5946}
    );

    @Data
    @Builder
    public static class SimulatedTrip {
        private String tripId;
        private String vehicleId;
        private String driverId;
        private String sourceName;
        private double sourceLat;
        private double sourceLng;
        private String destName;
        private double destLat;
        private double destLng;
        private double progress; // 0.0 to 1.0
        private int durationTicks; // total ticks at 1x
        private double speedKmh;
        private double heading;
        private double lastOdometerUpdate; // track last odometer value
    }

    @PostConstruct
    public void init() {
        // Find already dispatched trips on startup and simulation
        List<Trip> dispatchedTrips = tripRepository.findAll().stream()
                .filter(t -> t.getStatus() == TripStatus.DISPATCHED)
                .toList();

        for (Trip trip : dispatchedTrips) {
            registerTrip(trip, 0.2 + new Random().nextDouble() * 0.5); // start at random progress
        }
        log.info("Initialized simulation with {} active trips", activeTrips.size());
    }

    public void setSpeedMultiplier(int multiplier) {
        this.speedMultiplier = Math.max(1, Math.min(60, multiplier));
        log.info("Simulation speed multiplier set to: {}x", this.speedMultiplier);
    }

    public int getSpeedMultiplier() {
        return this.speedMultiplier;
    }

    public void registerTrip(Trip trip, double initialProgress) {
        double[] src = DEPOT_COORDS.getOrDefault(trip.getSource(), new double[]{23.2156, 72.6369});
        double[] dest = DEPOT_COORDS.getOrDefault(trip.getDestination(), new double[]{23.0225, 72.5714});
        double heading = calculateHeading(src[0], src[1], dest[0], dest[1]);
        SimulatedTrip sim = SimulatedTrip.builder()
                .tripId(trip.getId())
                .vehicleId(trip.getVehicleId())
                .driverId(trip.getDriverId())
                .sourceName(trip.getSource())
                .sourceLat(src[0])
                .sourceLng(src[1])
                .destName(trip.getDestination())
                .destLat(dest[0])
                .destLng(dest[1])
                .progress(initialProgress)
                .durationTicks(120 + new Random().nextInt(60)) // ~2 mins trip
                .speedKmh(60.0 + new Random().nextDouble() * 20.0)
                .heading(heading)
                .lastOdometerUpdate(0.0)
                .build();
        activeTrips.put(trip.getId(), sim);
        log.info("Registered simulated trip: {} for vehicle: {}", trip.getId(), trip.getVehicleId());
    }

    @Scheduled(fixedRate = 1000)
    public void tick() {
        if (activeTrips.isEmpty()) {
            return;
        }

        long ts = System.currentTimeMillis();
        for (SimulatedTrip sim : activeTrips.values()) {
            // Verify trip state in DB has not changed (e.g. cancelled/completed outside simulator)
            Optional<Trip> tripOpt = tripRepository.findById(sim.getTripId());
            if (tripOpt.isEmpty() || tripOpt.get().getStatus() != TripStatus.DISPATCHED) {
                activeTrips.remove(sim.getTripId());
                continue;
            }

            // Update progress
            double step = (1.0 / sim.getDurationTicks()) * speedMultiplier;
            double nextProgress = Math.min(1.0, sim.getProgress() + step);
            sim.setProgress(nextProgress);

            // Interpolate position
            double jitterLat = (new Random().nextDouble() - 0.5) * 0.0006;
            double jitterLng = (new Random().nextDouble() - 0.5) * 0.0006;
            double currentLat = sim.getSourceLat() + (sim.getDestLat() - sim.getSourceLat()) * nextProgress + jitterLat;
            double currentLng = sim.getSourceLng() + (sim.getDestLng() - sim.getSourceLng()) * nextProgress + jitterLng;

            // Fluctuating speed
            double currentSpeed = sim.getSpeedKmh() + (new Random().nextDouble() - 0.5) * 5.0;
            currentSpeed = Math.max(30.0, Math.min(100.0, currentSpeed));

            // Publish positions
            Map<String, Object> posMsg = new HashMap<>();
            posMsg.put("vehicleId", sim.getVehicleId());
            posMsg.put("lat", currentLat);
            posMsg.put("lng", currentLng);
            posMsg.put("speedKmh", currentSpeed);
            posMsg.put("heading", sim.getHeading());
            posMsg.put("tripId", sim.getTripId());
            posMsg.put("ts", ts);
            messagingTemplate.convertAndSend("/topic/vehicle-position", posMsg);

            // Update Vehicle metrics in DB
            Optional<Vehicle> vOpt = vehicleRepository.findById(sim.getVehicleId());
            if (vOpt.isPresent()) {
                Vehicle vehicle = vOpt.get();
                VehicleHealth health = vehicle.getHealth();

                // Degrade health
                if (health != null) {
                    health.setTyres(Math.max(10, health.getTyres() - new Random().nextDouble() * 0.05 * speedMultiplier));
                    health.setEngine(Math.max(10, health.getEngine() - new Random().nextDouble() * 0.03 * speedMultiplier));
                    health.setBrakes(Math.max(10, health.getBrakes() - new Random().nextDouble() * 0.04 * speedMultiplier));
                    double avg = (health.getTyres() + health.getEngine() + health.getBrakes()) / 3.0;
                    health.setRiskScore(Math.min(99, Math.round(100.0 - avg)));
                }

                // Add to odometer (approximation based on speed)
                double distKm = (currentSpeed * (1.0 / 3600.0)) * speedMultiplier;
                vehicle.setOdometer(vehicle.getOdometer() + distKm);
                vehicleRepository.save(vehicle);

                // Publish health update every ~5 ticks
                if (new Random().nextInt(5) == 0 && health != null) {
                    Map<String, Object> healthMsg = new HashMap<>();
                    healthMsg.put("vehicleId", sim.getVehicleId());
                    healthMsg.put("tyres", Math.round(health.getTyres()));
                    healthMsg.put("engine", Math.round(health.getEngine()));
                    healthMsg.put("brakes", Math.round(health.getBrakes()));
                    healthMsg.put("riskScore", (int) health.getRiskScore());
                    healthMsg.put("ts", ts);
                    messagingTemplate.convertAndSend("/topic/vehicle-health", healthMsg);
                }
            }

            // Check if finished
            if (nextProgress >= 1.0) {
                completeTrip(sim, ts);
            }
        }
    }

    @Scheduled(fixedRate = 2000)
    public void pushKpi() {
        long ts = System.currentTimeMillis();
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        List<Driver> allDrivers = driverRepository.findAll();
        List<Trip> allTrips = tripRepository.findAll();

        long activeVehicles = allVehicles.stream().filter(v -> v.getStatus() == VehicleStatus.ON_TRIP).count();
        long availableVehicles = allVehicles.stream().filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count();
        long inMaintenance = allVehicles.stream().filter(v -> v.getStatus() == VehicleStatus.IN_SHOP).count();
        long activeTripsCount = allTrips.stream().filter(t -> t.getStatus() == TripStatus.DISPATCHED).count();
        long pendingTripsCount = allTrips.stream().filter(t -> t.getStatus() == TripStatus.DRAFT).count();
        long driversOnDuty = allDrivers.stream().filter(d -> d.getStatus() == DriverStatus.AVAILABLE || d.getStatus() == DriverStatus.ON_TRIP).count();

        long nonRetired = allVehicles.stream().filter(v -> v.getStatus() != VehicleStatus.RETIRED).count();
        double utilizationPct = nonRetired == 0 ? 0.0 : Math.round(((double) activeVehicles / nonRetired) * 1000.0) / 10.0;

        Map<String, Object> kpiMsg = new HashMap<>();
        kpiMsg.put("activeVehicles", activeVehicles);
        kpiMsg.put("availableVehicles", availableVehicles);
        kpiMsg.put("inMaintenance", inMaintenance);
        kpiMsg.put("activeTrips", activeTripsCount);
        kpiMsg.put("pendingTrips", pendingTripsCount);
        kpiMsg.put("driversOnDuty", driversOnDuty);
        kpiMsg.put("utilizationPct", utilizationPct);
        kpiMsg.put("ts", ts);

        messagingTemplate.convertAndSend("/topic/kpi", kpiMsg);
    }

    private void completeTrip(SimulatedTrip sim, long ts) {
        activeTrips.remove(sim.getTripId());

        Optional<Trip> tripOpt = tripRepository.findById(sim.getTripId());
        Optional<Vehicle> vOpt = vehicleRepository.findById(sim.getVehicleId());
        Optional<Driver> dOpt = driverRepository.findById(sim.getDriverId());

        if (tripOpt.isPresent() && vOpt.isPresent() && dOpt.isPresent()) {
            Trip trip = tripOpt.get();
            Vehicle vehicle = vOpt.get();
            Driver driver = dOpt.get();

            trip.setStatus(TripStatus.COMPLETED);
            trip.setCompletedAt(Instant.ofEpochMilli(ts));
            tripRepository.save(trip);

            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);

            driver.setStatus(DriverStatus.AVAILABLE);
            driverRepository.save(driver);

            // Log mock fuel and expenses on trip completion
            double liters = 30.0 + new Random().nextDouble() * 20.0;
            double cost = liters * 95.0; // ₹95 per liter
            FuelLog fuelLog = FuelLog.builder()
                    .id(UUID.randomUUID().toString())
                    .vehicleId(vehicle.getId())
                    .tripId(trip.getId())
                    .liters(Math.round(liters * 10.0) / 10.0)
                    .cost(Math.round(cost))
                    .odometer(vehicle.getOdometer())
                    .loggedAt(Instant.ofEpochMilli(ts))
                    .build();
            fuelLogRepository.save(fuelLog);

            Expense toll = Expense.builder()
                    .id(UUID.randomUUID().toString())
                    .vehicleId(vehicle.getId())
                    .tripId(trip.getId())
                    .category(ExpenseCategory.TOLL)
                    .amount(120.0)
                    .description("Highway Toll charge")
                    .incurredAt(Instant.ofEpochMilli(ts))
                    .build();
            expenseRepository.save(toll);

            // Publish WebSocket updates
            Map<String, Object> tripEvent = new HashMap<>();
            tripEvent.put("tripId", trip.getId());
            tripEvent.put("status", "COMPLETED");
            tripEvent.put("vehicleId", vehicle.getId());
            tripEvent.put("driverId", driver.getId());
            tripEvent.put("ts", ts);
            messagingTemplate.convertAndSend("/topic/trip-status", tripEvent);

            Map<String, Object> vehStatusEvent = new HashMap<>();
            vehStatusEvent.put("vehicleId", vehicle.getId());
            vehStatusEvent.put("status", "AVAILABLE");
            vehStatusEvent.put("ts", ts);
            messagingTemplate.convertAndSend("/topic/vehicle-status", vehStatusEvent);

            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "TRIP_COMPLETED");
            alert.put("severity", "INFO");
            alert.put("message", vehicle.getRegNumber() + " completed " + trip.getSource() + " -> " + trip.getDestination());
            alert.put("vehicleId", vehicle.getId());
            alert.put("tripId", trip.getId());
            alert.put("ts", ts);
            messagingTemplate.convertAndSend("/topic/alerts", alert);

            log.info("Completed simulated trip: {}", sim.getTripId());
        }
    }

    public void triggerBreakdown(String vehicleId) {
        SimulatedTrip targetSim = null;
        for (SimulatedTrip sim : activeTrips.values()) {
            if (sim.getVehicleId().equals(vehicleId)) {
                targetSim = sim;
                break;
            }
        }

        if (targetSim == null) {
            log.warn("Cannot trigger breakdown: Vehicle {} has no active simulated trip", vehicleId);
            return;
        }

        activeTrips.remove(targetSim.getTripId());
        long ts = System.currentTimeMillis();

        Optional<Trip> tripOpt = tripRepository.findById(targetSim.getTripId());
        Optional<Vehicle> vOpt = vehicleRepository.findById(targetSim.getVehicleId());
        Optional<Driver> dOpt = driverRepository.findById(targetSim.getDriverId());

        if (tripOpt.isPresent() && vOpt.isPresent() && dOpt.isPresent()) {
            Trip trip = tripOpt.get();
            Vehicle vehicle = vOpt.get();
            Driver driver = dOpt.get();

            trip.setStatus(TripStatus.INTERRUPTED);
            tripRepository.save(trip);

            vehicle.setStatus(VehicleStatus.BROKEN_DOWN);
            VehicleHealth health = vehicle.getHealth();
            if (health != null) {
                health.setEngine(15.0);
                health.setRiskScore(91.0);
            }
            vehicleRepository.save(vehicle);

            // Publish status updates
            Map<String, Object> tripEvent = new HashMap<>();
            tripEvent.put("tripId", trip.getId());
            tripEvent.put("status", "INTERRUPTED");
            tripEvent.put("vehicleId", vehicle.getId());
            tripEvent.put("driverId", driver.getId());
            tripEvent.put("ts", ts);
            messagingTemplate.convertAndSend("/topic/trip-status", tripEvent);

            Map<String, Object> vehStatusEvent = new HashMap<>();
            vehStatusEvent.put("vehicleId", vehicle.getId());
            vehStatusEvent.put("status", "BROKEN_DOWN");
            vehStatusEvent.put("ts", ts);
            messagingTemplate.convertAndSend("/topic/vehicle-status", vehStatusEvent);

            if (health != null) {
                Map<String, Object> healthMsg = new HashMap<>();
                healthMsg.put("vehicleId", vehicle.getId());
                healthMsg.put("tyres", Math.round(health.getTyres()));
                healthMsg.put("engine", 15);
                healthMsg.put("brakes", Math.round(health.getBrakes()));
                healthMsg.put("riskScore", 91);
                healthMsg.put("ts", ts);
                messagingTemplate.convertAndSend("/topic/vehicle-health", healthMsg);
            }

            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "BREAKDOWN");
            alert.put("severity", "CRITICAL");
            alert.put("message", vehicle.getRegNumber() + " broke down en route to " + trip.getDestination());
            alert.put("vehicleId", vehicle.getId());
            alert.put("tripId", trip.getId());
            alert.put("ts", ts);
            messagingTemplate.convertAndSend("/topic/alerts", alert);

            log.info("Triggered breakdown for vehicle: {} on trip: {}", vehicleId, trip.getId());

            // Towing charge expense
            Expense tow = Expense.builder()
                    .id(UUID.randomUUID().toString())
                    .vehicleId(vehicle.getId())
                    .tripId(trip.getId())
                    .category(ExpenseCategory.OTHER)
                    .amount(2500.0)
                    .description("Towing charge after breakdown")
                    .incurredAt(Instant.ofEpochMilli(ts))
                    .build();
            expenseRepository.save(tow);

            // Queue a rescue dispatch
            double currentLat = targetSim.getSourceLat() + (targetSim.getDestLat() - targetSim.getSourceLat()) * targetSim.getProgress();
            double currentLng = targetSim.getSourceLng() + (targetSim.getDestLng() - targetSim.getSourceLng()) * targetSim.getProgress();
            queueRescue(trip, currentLat, currentLng, ts);
        }
    }

    private void queueRescue(Trip brokenTrip, double lat, double lng, long breakTs) {
        rescueScheduler.schedule(() -> {
            try {
                // Find nearest available vehicle and driver
                List<Vehicle> vehicles = vehicleRepository.findAll().stream()
                        .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                        .toList();

                List<Driver> drivers = driverRepository.findAll().stream()
                        .filter(d -> d.getStatus() == DriverStatus.AVAILABLE && d.getLicenseExpiry().isAfter(java.time.LocalDate.now()))
                        .toList();

                if (vehicles.isEmpty() || drivers.isEmpty()) {
                    log.warn("Rescue dispatch failed: No available vehicles or drivers");
                    return;
                }

                // Pick the first available pairs (Haversine fallback - simple pick matches fast demo requirement)
                Vehicle rescueVehicle = vehicles.get(0);
                Driver rescueDriver = drivers.get(0);

                rescueVehicle.setStatus(VehicleStatus.ON_TRIP);
                vehicleRepository.save(rescueVehicle);

                rescueDriver.setStatus(DriverStatus.ON_TRIP);
                driverRepository.save(rescueDriver);

                double[] brokenDest = DEPOT_COORDS.getOrDefault(brokenTrip.getDestination(), new double[]{23.0225, 72.5714});
                Trip rescueTrip = Trip.builder()
                        .id(UUID.randomUUID().toString())
                        .source("Breakdown Point (" + rescueVehicle.getRegNumber() + ")")
                        .destination(brokenTrip.getDestination())
                        .vehicleId(rescueVehicle.getId())
                        .driverId(rescueDriver.getId())
                        .cargoWeightKg(brokenTrip.getCargoWeightKg())
                        .status(TripStatus.DISPATCHED)
                        .routePolyline(routeService.getRoutePolyline(lat, lng, brokenDest[0], brokenDest[1]))
                        .createdAt(Instant.now())
                        .dispatchedAt(Instant.now())
                        .build();

                tripRepository.save(rescueTrip);

                // Register in the active simulation
                registerTrip(rescueTrip, 0.0);

                long ts = System.currentTimeMillis();

                // Publish WebSocket updates
                Map<String, Object> tripEvent = new HashMap<>();
                tripEvent.put("tripId", rescueTrip.getId());
                tripEvent.put("status", "DISPATCHED");
                tripEvent.put("vehicleId", rescueVehicle.getId());
                tripEvent.put("driverId", rescueDriver.getId());
                tripEvent.put("ts", ts);
                messagingTemplate.convertAndSend("/topic/trip-status", tripEvent);

                Map<String, Object> vehStatusEvent = new HashMap<>();
                vehStatusEvent.put("vehicleId", rescueVehicle.getId());
                vehStatusEvent.put("status", "ON_TRIP");
                vehStatusEvent.put("ts", ts);
                messagingTemplate.convertAndSend("/topic/vehicle-status", vehStatusEvent);

                Map<String, Object> alert = new HashMap<>();
                alert.put("type", "RESCUE_DISPATCHED");
                alert.put("severity", "WARN");
                alert.put("message", "Rescue vehicle " + rescueVehicle.getRegNumber() + " dispatched for " + brokenTrip.getVehicleId());
                alert.put("vehicleId", rescueVehicle.getId());
                alert.put("tripId", rescueTrip.getId());
                alert.put("ts", ts);
                messagingTemplate.convertAndSend("/topic/alerts", alert);

                log.info("Rescue trip dispatched: {} using vehicle: {}", rescueTrip.getId(), rescueVehicle.getRegNumber());

            } catch (Exception ex) {
                log.error("Error in rescue queue: ", ex);
            }
        }, 8, TimeUnit.SECONDS);
    }

    public void spawnTrip() {
        List<Vehicle> vehicles = vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                .toList();

        List<Driver> drivers = driverRepository.findAll().stream()
                .filter(d -> d.getStatus() == DriverStatus.AVAILABLE && d.getLicenseExpiry().isAfter(java.time.LocalDate.now()))
                .toList();

        if (vehicles.isEmpty() || drivers.isEmpty()) {
            log.warn("Cannot spawn simulated trip: No available vehicles or drivers");
            return;
        }

        Vehicle v = vehicles.get(new Random().nextInt(vehicles.size()));
        Driver d = drivers.get(new Random().nextInt(drivers.size()));

        // Random depots from Ahmedabad / Gandhinagar coords
        double[][] points = {
                {23.2156, 72.6369}, // Gandhinagar
                {23.0225, 72.5714}, // Ahmedabad
                {22.9676, 72.6469}, // Vatva
                {22.9922, 72.3822}, // Sanand
                {23.2422, 72.4946}, // Kalol
                {23.4256, 72.6567}  // Mansa
        };
        String[] names = {"Gandhinagar Depot", "Ahmedabad Hub", "Vatva Industrial Area", "Sanand Warehouse", "Kalol Depot", "Mansa Yard"};

        int startIdx = new Random().nextInt(points.length);
        int destIdx = new Random().nextInt(points.length);
        while (startIdx == destIdx) {
            destIdx = new Random().nextInt(points.length);
        }

        v.setStatus(VehicleStatus.ON_TRIP);
        vehicleRepository.save(v);

        d.setStatus(DriverStatus.ON_TRIP);
        driverRepository.save(d);

        Trip trip = Trip.builder()
                .id(UUID.randomUUID().toString())
                .source(names[startIdx])
                .destination(names[destIdx])
                .vehicleId(v.getId())
                .driverId(d.getId())
                .cargoWeightKg(100.0 + new Random().nextInt(500))
                .status(TripStatus.DISPATCHED)
                .routePolyline(routeService.getRoutePolyline(points[startIdx][0], points[startIdx][1], points[destIdx][0], points[destIdx][1]))
                .createdAt(Instant.now())
                .dispatchedAt(Instant.now())
                .build();

        tripRepository.save(trip);

        registerTrip(trip, 0.0);

        long ts = System.currentTimeMillis();

        // Publish WebSocket updates
        Map<String, Object> tripEvent = new HashMap<>();
        tripEvent.put("tripId", trip.getId());
        tripEvent.put("status", "DISPATCHED");
        tripEvent.put("vehicleId", v.getId());
        tripEvent.put("driverId", d.getId());
        tripEvent.put("ts", ts);
        messagingTemplate.convertAndSend("/topic/trip-status", tripEvent);

        Map<String, Object> vehStatusEvent = new HashMap<>();
        vehStatusEvent.put("vehicleId", v.getId());
        vehStatusEvent.put("status", "ON_TRIP");
        vehStatusEvent.put("ts", ts);
        messagingTemplate.convertAndSend("/topic/vehicle-status", vehStatusEvent);

        log.info("Simulated trip spawned: {}", trip.getId());
    }

    private double calculateHeading(double lat1, double lng1, double lat2, double lng2) {
        double dLng = Math.toRadians(lng2 - lng1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double y = Math.sin(dLng) * Math.cos(rLat2);
        double x = Math.cos(rLat1) * Math.sin(rLat2) - Math.sin(rLat1) * Math.cos(rLat2) * Math.cos(dLng);
        double brng = Math.atan2(y, x);

        return (Math.toDegrees(brng) + 360.0) % 360.0;
    }
}
