package com.transitops.domain.trip.entity;

import com.transitops.common.entity.AssignedIdEntity;
import com.transitops.common.enums.TripStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Trip extends AssignedIdEntity {

    private String source;         // kept for backward compat / display
    private String destination;

    private String sourceName;
    private double sourceLat;
    private double sourceLng;

    private String destinationName;
    private double destinationLat;
    private double destinationLng;

    private String vehicleId;
    private String driverId;

    private double cargoWeightKg;
    private double plannedDistanceKm;
    private Double actualDistanceKm;

    @Enumerated(EnumType.STRING)
    private TripStatus status;

    private String routePolyline;

    private Instant createdAt;
    private Instant dispatchedAt;
    private Instant completedAt;
}