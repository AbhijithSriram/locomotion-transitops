package com.transitops.domain.trip.service;

import com.transitops.common.enums.TripStatus;
import com.transitops.common.exception.NotFoundException;
import com.transitops.common.util.Ids;
import com.transitops.domain.trip.dto.TripCreateRequest;
import com.transitops.domain.trip.dto.TripResponse;
import com.transitops.domain.trip.entity.Trip;
import com.transitops.domain.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;

    @Transactional
    public TripResponse create(TripCreateRequest request) {
        Trip trip = Trip.builder()
                .id(Ids.newId())
                .source(request.source())
                .destination(request.destination())
                .vehicleId(request.vehicleId())
                .driverId(request.driverId())
                .cargoWeightKg(request.cargoWeightKg())
                .plannedDistanceKm(request.plannedDistanceKm())
                .routePolyline(request.routePolyline())
                .status(TripStatus.DRAFT)
                .createdAt(Instant.now())
                .build();

        return toResponse(tripRepository.save(trip));
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findAll() {
        return tripRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TripResponse findById(String id) {
        return toResponse(tripRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + id)));
    }

    public TripResponse toResponse(Trip t) {
        return new TripResponse(
                t.getId(), t.getSource(), t.getDestination(), t.getVehicleId(), t.getDriverId(),
                t.getCargoWeightKg(), t.getPlannedDistanceKm(), t.getActualDistanceKm(), t.getStatus(),
                t.getRoutePolyline(), t.getCreatedAt(), t.getDispatchedAt(), t.getCompletedAt()
        );
    }
}