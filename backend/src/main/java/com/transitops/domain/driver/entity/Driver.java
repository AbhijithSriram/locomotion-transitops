package com.transitops.domain.driver.entity;

import com.transitops.common.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver implements Persistable<String> {

    @Id
    private String id;

    private String name;

    @Column(unique = true, nullable = false)
    private String licenseNumber;

    private String licenseCategory;

    private String contact;

    private LocalDate licenseExpiry;

    @Builder.Default
    private double safetyScore = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status;

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