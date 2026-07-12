package com.transitops.domain.finance.entity;

import com.transitops.common.entity.AssignedIdEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "fuel_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FuelLog extends AssignedIdEntity {
    private String vehicleId;
    private String tripId;
    private double liters;
    private double cost;
    private double odometer;
    private Instant date;
    private Instant loggedAt;
}