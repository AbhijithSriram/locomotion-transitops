package com.transitops.domain.sync.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitops.common.enums.TripStatus;
import com.transitops.common.exception.ConflictException;
import com.transitops.common.exception.NotFoundException;
import com.transitops.common.util.Ids;
import com.transitops.domain.finance.entity.FuelLog;
import com.transitops.domain.finance.repository.FuelLogRepository;
import com.transitops.domain.incident.entity.IncidentReport;
import com.transitops.domain.incident.repository.IncidentReportRepository;
import com.transitops.domain.sync.dto.SyncActionItem;
import com.transitops.domain.sync.dto.SyncActionResult;
import com.transitops.domain.sync.entity.SyncedAction;
import com.transitops.domain.sync.repository.SyncedActionRepository;
import com.transitops.domain.trip.entity.Trip;
import com.transitops.domain.trip.repository.TripRepository;
import com.transitops.domain.trip.service.DispatchService;
import com.transitops.domain.vehicle.entity.Vehicle;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SyncActionProcessor {

    private final SyncedActionRepository syncedActionRepository;
    private final DispatchService dispatchService;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelLogRepository fuelLogRepository;
    private final IncidentReportRepository incidentReportRepository;
    private final ObjectMapper objectMapper;

    /**
     * Each action runs in its own transaction so one failure
     * doesn't roll back the rest of the batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncActionResult processOne(SyncActionItem item) {
        try {
            if (syncedActionRepository.findById(item.idempotencyKey()).isPresent()) {
                return SyncActionResult.applied(item.idempotencyKey());
            }

            SyncActionResult result = switch (item.type()) {
                case "TRIP_COMPLETE" -> handleTripComplete(item);
                case "FUEL_LOG" -> handleFuelLog(item);
                case "INCIDENT_REPORT" -> handleIncidentReport(item);
                case "ODOMETER_UPDATE" -> handleOdometerUpdate(item);
                default -> SyncActionResult.error(
                        item.idempotencyKey(),
                        "Unknown action type: " + item.type()
                );
            };

            if (!"error".equals(result.result())) {
                syncedActionRepository.save(
                        SyncedAction.builder()
                                .idempotencyKey(item.idempotencyKey())
                                .driverId(item.driverId())
                                .type(item.type())
                                .payloadJson(item.payload().toString())
                                .processedAt(Instant.now())
                                .build()
                );
            }

            return result;

        } catch (Exception e) {
            return SyncActionResult.error(item.idempotencyKey(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Status-changing actions
    // -------------------------------------------------------------------------

    private SyncActionResult handleTripComplete(SyncActionItem item) {
        JsonNode payload = item.payload();

        String tripId = requireField(payload, "tripId");
        double finalOdometer = payload.get("finalOdometer").asDouble();

        Double actualDistanceKm = payload.hasNonNull("actualDistanceKm")
                ? payload.get("actualDistanceKm").asDouble()
                : null;

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + tripId));

        if (trip.getStatus() != TripStatus.DISPATCHED) {
            return SyncActionResult.conflict(
                    item.idempotencyKey(),
                    "Trip is " + trip.getStatus() + ", expected DISPATCHED"
            );
        }

        try {
            dispatchService.complete(tripId, finalOdometer, actualDistanceKm);
            return SyncActionResult.applied(item.idempotencyKey());
        } catch (ConflictException e) {
            return SyncActionResult.conflict(item.idempotencyKey(), e.getMessage());
        }
    }

    private SyncActionResult handleOdometerUpdate(SyncActionItem item) {
        JsonNode payload = item.payload();

        String vehicleId = requireField(payload, "vehicleId");
        double odometer = payload.get("odometer").asDouble();

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId));

        if (odometer < vehicle.getOdometer()) {
            return SyncActionResult.conflict(
                    item.idempotencyKey(),
                    "Server odometer (" + vehicle.getOdometer()
                            + ") is ahead of submitted value (" + odometer + ")"
            );
        }

        vehicle.setOdometer(odometer);
        vehicleRepository.save(vehicle);

        return SyncActionResult.applied(item.idempotencyKey());
    }

    // -------------------------------------------------------------------------
    // Append-only actions
    // -------------------------------------------------------------------------

    private SyncActionResult handleFuelLog(SyncActionItem item) {
        JsonNode payload = item.payload();

        String vehicleId = requireField(payload, "vehicleId");
        double liters = payload.get("liters").asDouble();
        double cost = payload.get("cost").asDouble();

        Instant date = payload.hasNonNull("date")
                ? Instant.parse(payload.get("date").asText())
                : Instant.now();

        FuelLog log = FuelLog.builder()
                .id(Ids.newId())
                .vehicleId(vehicleId)
                .liters(liters)
                .cost(cost)
                .date(date)
                .build();

        fuelLogRepository.save(log);

        return SyncActionResult.applied(item.idempotencyKey());
    }

    private SyncActionResult handleIncidentReport(SyncActionItem item) {
        JsonNode payload = item.payload();

        String vehicleId = requireField(payload, "vehicleId");
        String description = requireField(payload, "description");

        String tripId = payload.hasNonNull("tripId")
                ? payload.get("tripId").asText()
                : null;

        IncidentReport report = IncidentReport.builder()
                .id(Ids.newId())
                .vehicleId(vehicleId)
                .driverId(item.driverId())
                .tripId(tripId)
                .description(description)
                .reportedAt(Instant.now())
                .build();

        incidentReportRepository.save(report);

        return SyncActionResult.applied(item.idempotencyKey());
    }

    // -------------------------------------------------------------------------

    private String requireField(JsonNode payload, String field) {
        if (!payload.hasNonNull(field)) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return payload.get(field).asText();
    }
}