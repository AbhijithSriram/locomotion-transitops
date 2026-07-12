package com.transitops.domain.trip;

import com.transitops.common.enums.DriverStatus;
import com.transitops.common.enums.TripStatus;
import com.transitops.common.enums.VehicleStatus;
import com.transitops.common.exception.ConflictException;
import com.transitops.domain.driver.entity.Driver;
import com.transitops.domain.driver.repository.DriverRepository;
import com.transitops.domain.trip.entity.Trip;
import com.transitops.domain.trip.repository.TripRepository;
import com.transitops.domain.trip.service.DispatchService;
import com.transitops.domain.vehicle.entity.Vehicle;
import com.transitops.domain.vehicle.entity.VehicleHealth;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import com.transitops.domain.maintenance.repository.MaintenanceLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private MaintenanceLogRepository maintenanceLogRepository;

    @InjectMocks
    private DispatchService dispatchService;

    private Vehicle vehicle;
    private Driver driver;
    private Trip trip;

    @BeforeEach
    void setUp() {
        vehicle = Vehicle.builder()
                .id("v1").regNumber("KA01AB1234").maxLoadKg(5000)
                .status(VehicleStatus.AVAILABLE).odometer(1000)
                .health(VehicleHealth.builder().vehicleId("v1").build())
                .build();

        driver = Driver.builder()
                .id("d1").licenseNumber("DL123").status(DriverStatus.AVAILABLE)
                .licenseExpiry(LocalDate.now().plusYears(1))
                .build();

        trip = Trip.builder()
                .id("t1").vehicleId("v1").driverId("d1")
                .cargoWeightKg(2000).plannedDistanceKm(100)
                .status(TripStatus.DRAFT)
                .build();

        lenient().when(tripRepository.findById("t1")).thenReturn(Optional.of(trip));
        lenient().when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle));
        lenient().when(driverRepository.findById("d1")).thenReturn(Optional.of(driver));
        lenient().when(tripRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(driverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void dispatch_happyPath_setsAllStatusesAndTimestamps() {
        Trip result = dispatchService.dispatch("t1");

        assertThat(result.getStatus()).isEqualTo(TripStatus.DISPATCHED);
        assertThat(result.getDispatchedAt()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ON_TRIP);
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.ON_TRIP);
    }

    @Test
    void dispatch_vehicleInShop_throwsConflict() {
        vehicle.setStatus(VehicleStatus.IN_SHOP);
        assertThatThrownBy(() -> dispatchService.dispatch("t1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Vehicle not available");
    }

    @Test
    void dispatch_vehicleRetired_throwsConflict() {
        vehicle.setStatus(VehicleStatus.RETIRED);
        assertThatThrownBy(() -> dispatchService.dispatch("t1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void dispatch_driverSuspended_throwsConflict() {
        driver.setStatus(DriverStatus.SUSPENDED);
        assertThatThrownBy(() -> dispatchService.dispatch("t1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Driver not available");
    }

    @Test
    void dispatch_expiredLicense_throwsConflict() {
        driver.setLicenseExpiry(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> dispatchService.dispatch("t1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("license expired");
    }

    @Test
    void dispatch_vehicleAlreadyOnTrip_throwsConflict() {
        vehicle.setStatus(VehicleStatus.ON_TRIP);
        assertThatThrownBy(() -> dispatchService.dispatch("t1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void dispatch_driverAlreadyOnTrip_throwsConflict() {
        driver.setStatus(DriverStatus.ON_TRIP);
        assertThatThrownBy(() -> dispatchService.dispatch("t1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void dispatch_cargoExceedsCapacity_throwsConflict() {
        trip.setCargoWeightKg(9999);
        assertThatThrownBy(() -> dispatchService.dispatch("t1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cargo weight exceeds");
    }

    @Test
    void dispatch_tripNotDraft_throwsConflict() {
        trip.setStatus(TripStatus.DISPATCHED);
        assertThatThrownBy(() -> dispatchService.dispatch("t1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void complete_happyPath_restoresAvailabilityAndSetsOdometer() {
        trip.setStatus(TripStatus.DISPATCHED);
        vehicle.setStatus(VehicleStatus.ON_TRIP);
        driver.setStatus(DriverStatus.ON_TRIP);

        Trip result = dispatchService.complete("t1", 1200, 95.0);

        assertThat(result.getStatus()).isEqualTo(TripStatus.COMPLETED);
        assertThat(result.getActualDistanceKm()).isEqualTo(95.0);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.getOdometer()).isEqualTo(1200);
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void complete_notDispatched_throwsConflict() {
        trip.setStatus(TripStatus.DRAFT);
        assertThatThrownBy(() -> dispatchService.complete("t1", 1200, 95.0))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_happyPath_restoresAvailability() {
        trip.setStatus(TripStatus.DISPATCHED);
        vehicle.setStatus(VehicleStatus.ON_TRIP);
        driver.setStatus(DriverStatus.ON_TRIP);

        Trip result = dispatchService.cancel("t1");

        assertThat(result.getStatus()).isEqualTo(TripStatus.CANCELLED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }

    @Test
    void cancel_draftTrip_throwsConflict() {
        assertThatThrownBy(() -> dispatchService.cancel("t1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void markBrokenDown_happyPath_driverStaysOnTrip() {
        trip.setStatus(TripStatus.DISPATCHED);
        vehicle.setStatus(VehicleStatus.ON_TRIP);
        driver.setStatus(DriverStatus.ON_TRIP);

        when(tripRepository.findByVehicleIdAndStatus("v1", TripStatus.DISPATCHED))
                .thenReturn(Optional.of(trip));

        dispatchService.markBrokenDown("v1");

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.BROKEN_DOWN);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.INTERRUPTED);
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.ON_TRIP); // stranded, not freed
    }

    @Test
    void markBrokenDown_vehicleNotOnTrip_throwsConflict() {
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        assertThatThrownBy(() -> dispatchService.markBrokenDown("v1"))
                .isInstanceOf(ConflictException.class);
    }
    @Test
    void openMaintenance_setsVehicleInShop() {
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle));
        when(maintenanceLogRepository.findByVehicleIdAndStatus("v1", "active")).thenReturn(Optional.empty());
        when(maintenanceLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var log = dispatchService.openMaintenance("v1", "SERVICE", 500);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_SHOP);
        assertThat(log.getStatus()).isEqualTo("active");
    }

    @Test
    void openMaintenance_retiredVehicle_throwsConflict() {
        vehicle.setStatus(VehicleStatus.RETIRED);
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> dispatchService.openMaintenance("v1", "SERVICE", 500))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void closeMaintenance_restoresAvailable() {
        vehicle.setStatus(VehicleStatus.IN_SHOP);
        var log = com.transitops.domain.maintenance.entity.MaintenanceLog.builder()
                .id("m1").vehicleId("v1").status("active").cost(500)
                .build();

        when(maintenanceLogRepository.findById("m1")).thenReturn(Optional.of(log));
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle));
        when(maintenanceLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = dispatchService.closeMaintenance("m1");

        assertThat(result.getStatus()).isEqualTo("closed");
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void closeMaintenance_retiredVehicle_staysRetired() {
        vehicle.setStatus(VehicleStatus.RETIRED);
        var log = com.transitops.domain.maintenance.entity.MaintenanceLog.builder()
                .id("m1").vehicleId("v1").status("active").cost(500)
                .build();

        when(maintenanceLogRepository.findById("m1")).thenReturn(Optional.of(log));
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle));
        when(maintenanceLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dispatchService.closeMaintenance("m1");

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RETIRED);
    }
}