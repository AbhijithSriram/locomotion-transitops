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

import com.transitops.simulation.service.RouteService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final RouteService routeService;

    // Known depot coordinates; REST clients send location names only, but the
    // simulation needs lat/lng to animate the trip on the map.
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

    @Transactional
    public TripResponse create(TripCreateRequest request) {
        double[] src = DEPOT_COORDS.getOrDefault(request.source(), new double[]{23.2156, 72.6369});
        double[] dest = DEPOT_COORDS.getOrDefault(request.destination(), new double[]{23.0225, 72.5714});
        String polyline = request.routePolyline() != null
                ? request.routePolyline()
                : routeService.getRoutePolyline(src[0], src[1], dest[0], dest[1]);

        Trip trip = Trip.builder()
                .id(Ids.newId())
                .source(request.source())
                .destination(request.destination())
                .sourceName(request.source())
                .sourceLat(src[0])
                .sourceLng(src[1])
                .destinationName(request.destination())
                .destinationLat(dest[0])
                .destinationLng(dest[1])
                .vehicleId(request.vehicleId())
                .driverId(request.driverId())
                .cargoWeightKg(request.cargoWeightKg())
                .plannedDistanceKm(request.plannedDistanceKm())
                .routePolyline(polyline)
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