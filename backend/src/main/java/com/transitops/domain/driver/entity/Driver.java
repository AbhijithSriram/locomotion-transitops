package com.transitops.domain.driver.entity;

import com.transitops.common.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String licenseNumber;

    @Column(nullable = false)
    private Instant licenseExpiry;

    @Builder.Default
    private double safetyScore = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status;
}
