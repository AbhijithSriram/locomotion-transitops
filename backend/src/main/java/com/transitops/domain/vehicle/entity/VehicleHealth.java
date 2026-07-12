package com.transitops.domain.vehicle.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "vehicle_health")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleHealth {

    @Id
    private String vehicleId;

    @Builder.Default
    private double tyres = 100;

    @Builder.Default
    private double engine = 100;

    @Builder.Default
    private double brakes = 100;

    @Builder.Default
    private double riskScore = 0;

    private Instant lastServiceAt;
}