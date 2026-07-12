package com.transitops.domain.trip.service;

import com.transitops.common.enums.DriverStatus;
import com.transitops.common.enums.TripStatus;
import com.transitops.common.enums.VehicleStatus;
import com.transitops.common.events.TripStatusChanged;
import com.transitops.common.exception.ConflictException;
import com.transitops.common.exception.NotFoundException;
import com.transitops.domain.driver.entity.Driver;
import com.transitops.domain.driver.repository.DriverRepository;
import com.transitops.domain.trip.entity.Trip;
import com.transitops.domain.trip.repository.TripRepository;
import com.transitops.domain.vehicle.entity.Vehicle;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import com.transitops.domain.maintenance.entity.MaintenanceLog;
import com.transitops.domain.maintenance.repository.MaintenanceLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MaintenanceLogRepository maintenanceLogRepository;

    @Transactional
    public Trip dispatch(String tripId) {
        Trip trip = getTripOrThrow(tripId);

        if (trip.getStatus() != TripStatus.DRAFT) {
            throw new ConflictException("Trip must be DRAFT to dispatch, was: " + trip.getStatus());
        }

        Vehicle vehicle = getVehicleOrThrow(trip.getVehicleId());
        Driver driver = getDriverOrThrow(trip.getDriverId());

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ConflictException("Vehicle not available: " + vehicle.getStatus());
        }
        if (driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new ConflictException("Driver not available: " + driver.getStatus());
        }
        if (driver.getLicenseExpiry() != null && driver.getLicenseExpiry().isBefore(LocalDate.now())) {
            throw new ConflictException("Driver license expired");
        }
        if (trip.getCargoWeightKg() > vehicle.getMaxLoadKg()) {
            throw new ConflictException("Cargo weight exceeds vehicle capacity");
        }

        vehicle.setStatus(VehicleStatus.ON_TRIP);
        driver.setStatus(DriverStatus.ON_TRIP);
        trip.setStatus(TripStatus.DISPATCHED);
        trip.setDispatchedAt(Instant.now());

        vehicleRepository.save(vehicle);
        driverRepository.save(driver);
        Trip saved = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripStatusChanged(saved));
        return saved;
    }

    @Transactional
    public Trip complete(String tripId, double finalOdometer, Double actualDistanceKm) {
        Trip trip = getTripOrThrow(tripId);

        if (trip.getStatus() != TripStatus.DISPATCHED) {
            throw new ConflictException("Trip must be DISPATCHED to complete, was: " + trip.getStatus());
        }

        Vehicle vehicle = getVehicleOrThrow(trip.getVehicleId());
        Driver driver = getDriverOrThrow(trip.getDriverId());

        if (finalOdometer < vehicle.getOdometer()) {
            throw new ConflictException("finalOdometer cannot be less than current odometer");
        }

        vehicle.setOdometer(finalOdometer);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        driver.setStatus(DriverStatus.AVAILABLE);

        trip.setActualDistanceKm(actualDistanceKm != null ? actualDistanceKm : trip.getPlannedDistanceKm());
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(Instant.now());

        vehicleRepository.save(vehicle);
        driverRepository.save(driver);
        Trip saved = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripStatusChanged(saved));
        return saved;
    }

    @Transactional
    public Trip cancel(String tripId) {
        Trip trip = getTripOrThrow(tripId);

        if (trip.getStatus() != TripStatus.DISPATCHED) {
            throw new ConflictException("Only a DISPATCHED trip can be cancelled, was: " + trip.getStatus());
        }

        Vehicle vehicle = getVehicleOrThrow(trip.getVehicleId());
        Driver driver = getDriverOrThrow(trip.getDriverId());

        vehicle.setStatus(VehicleStatus.AVAILABLE);
        driver.setStatus(DriverStatus.AVAILABLE);
        trip.setStatus(TripStatus.CANCELLED);

        vehicleRepository.save(vehicle);
        driverRepository.save(driver);
        Trip saved = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripStatusChanged(saved));
        return saved;
    }

    /**
     * Entry point for the simulation layer (or, later, a real telemetry feed) to report
     * a vehicle breakdown. Deliberately takes only a vehicleId — no assumption baked in
     * about the caller being a simulator.
     */
    @Transactional
    public void markBrokenDown(String vehicleId) {
        Vehicle vehicle = getVehicleOrThrow(vehicleId);

        if (vehicle.getStatus() != VehicleStatus.ON_TRIP) {
            throw new ConflictException("Vehicle must be ON_TRIP to break down, was: " + vehicle.getStatus());
        }

        Trip trip = tripRepository.findByVehicleIdAndStatus(vehicleId, TripStatus.DISPATCHED)
                .orElseThrow(() -> new NotFoundException("No active dispatched trip for vehicle: " + vehicleId));

        vehicle.setStatus(VehicleStatus.BROKEN_DOWN);
        trip.setStatus(TripStatus.INTERRUPTED);
        // driver intentionally stays ON_TRIP — stranded, not free, per brief

        vehicleRepository.save(vehicle);
        Trip saved = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripStatusChanged(saved));
    }
    @Transactional
    public MaintenanceLog openMaintenance(String vehicleId, String type, double cost) {
        Vehicle vehicle = getVehicleOrThrow(vehicleId);

        if (vehicle.getStatus() == VehicleStatus.RETIRED) {
            throw new ConflictException("Cannot open maintenance on a RETIRED vehicle");
        }
        if (vehicle.getStatus() == VehicleStatus.ON_TRIP) {
            throw new ConflictException("Cannot open maintenance while vehicle is ON_TRIP");
        }
        if (maintenanceLogRepository.findByVehicleIdAndStatus(vehicleId, "active").isPresent()) {
            throw new ConflictException("Vehicle already has an active maintenance record");
        }

        MaintenanceLog log = MaintenanceLog.builder()
                .id(com.transitops.common.util.Ids.newId())
                .vehicleId(vehicleId)
                .type(type)
                .status("active")
                .cost(cost)
                .createdAt(Instant.now())
                .build();

        vehicle.setStatus(VehicleStatus.IN_SHOP);
        vehicleRepository.save(vehicle);

        return maintenanceLogRepository.save(log);
    }

    @Transactional
    public MaintenanceLog closeMaintenance(String maintenanceLogId) {
        MaintenanceLog log = maintenanceLogRepository.findById(maintenanceLogId)
                .orElseThrow(() -> new NotFoundException("Maintenance log not found: " + maintenanceLogId));

        if (!"active".equals(log.getStatus())) {
            throw new ConflictException("Maintenance log is not active, was: " + log.getStatus());
        }

        Vehicle vehicle = getVehicleOrThrow(log.getVehicleId());

        log.setStatus("closed");
        log.setClosedAt(Instant.now());

        if (vehicle.getStatus() != VehicleStatus.RETIRED) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        return maintenanceLogRepository.save(log);
    }

    private Trip getTripOrThrow(String id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + id));
    }

    private Vehicle getVehicleOrThrow(String id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));
    }

    private Driver getDriverOrThrow(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + id));
    }
}