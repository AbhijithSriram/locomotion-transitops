package com.transitops.domain.trip.entity;

import com.transitops.common.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    private String id;

    @Column(nullable = false)
    private String sourceName;

    private double sourceLat;
    private double sourceLng;

    @Column(nullable = false)
    private String destinationName;

    private double destinationLat;
    private double destinationLng;

    @Column(nullable = false)
    private String vehicleId;

    @Column(nullable = false)
    private String driverId;

    private double cargoWeightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Column(columnDefinition = "TEXT")
    private String routePolyline;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant dispatchedAt;
    private Instant completedAt;
}
