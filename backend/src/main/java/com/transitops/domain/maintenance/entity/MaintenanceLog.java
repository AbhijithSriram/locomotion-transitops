package com.transitops.domain.maintenance.entity;

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
@Table(name = "maintenance_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MaintenanceLog extends AssignedIdEntity {

    private String vehicleId;
    private String type;

    // "active" | "closed"
    private String status;

    private double cost;

    private Instant createdAt;
    private Instant closedAt;
}