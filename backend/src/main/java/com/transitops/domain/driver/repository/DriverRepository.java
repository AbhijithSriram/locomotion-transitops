package com.transitops.domain.driver.repository;

import com.transitops.domain.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, String> {
    boolean existsByLicenseNumber(String licenseNumber);
    Optional<Driver> findByLicenseNumber(String licenseNumber);
}
