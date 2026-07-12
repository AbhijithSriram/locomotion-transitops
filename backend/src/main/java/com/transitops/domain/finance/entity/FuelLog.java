package com.transitops.domain.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fuel_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelLog {

    @Id
    private String id;

    @Column(nullable = false)
    private String vehicleId;

    private String tripId;

    private double liters;

    private double cost;

    private double odometer;

    @Column(nullable = false)
    private Instant loggedAt;
}
