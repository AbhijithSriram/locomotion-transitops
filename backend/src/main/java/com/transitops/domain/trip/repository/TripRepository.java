package com.transitops.domain.trip.repository;

import com.transitops.common.enums.TripStatus;
import com.transitops.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, String> {
    Optional<Trip> findByVehicleIdAndStatus(String vehicleId, TripStatus status);
}
