package com.transitops.domain.maintenance.entity;

import com.transitops.common.enums.MaintenanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "maintenance_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceLog {

    @Id
    private String id;

    @Column(nullable = false)
    private String vehicleId;

    private String description;

    private double cost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status;

    @Column(nullable = false)
    private Instant openedAt;

    private Instant closedAt;
}
