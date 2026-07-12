package com.transitops.domain.vehicle.repository;

import com.transitops.domain.vehicle.entity.VehicleHealth;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleHealthRepository extends JpaRepository<VehicleHealth, String> {
}