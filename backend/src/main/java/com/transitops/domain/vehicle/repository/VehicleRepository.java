package com.transitops.domain.vehicle.repository;

import com.transitops.domain.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    boolean existsByRegNumber(String regNumber);
    Optional<Vehicle> findByRegNumber(String regNumber);
}