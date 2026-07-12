package com.transitops.domain.vehicle.entity;

import com.transitops.common.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle implements Persistable<String> {

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

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}