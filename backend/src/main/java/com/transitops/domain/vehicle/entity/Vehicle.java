package com.transitops.domain.vehicle.entity;

import com.transitops.common.enums.VehicleStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String regNumber;

    private String name;

    private String type;

    private double maxLoadKg;

    @Builder.Default
    private double odometer = 0;

    private double acquisitionCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @MapsId
    @JoinColumn(name = "id")
    private VehicleHealth health;

    @Builder.Default
    private String transportMode = "TRUCK";
}