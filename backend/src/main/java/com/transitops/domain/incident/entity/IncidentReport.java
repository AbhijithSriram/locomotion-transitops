package com.transitops.domain.incident.entity;

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
@Table(name = "incident_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class IncidentReport extends AssignedIdEntity {
    private String vehicleId;
    private String driverId;
    private String tripId;
    private String description;
    private Instant reportedAt;
}