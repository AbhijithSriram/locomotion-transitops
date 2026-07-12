package com.transitops.domain.maintenance.repository;

import com.transitops.domain.maintenance.entity.MaintenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, String> {
    Optional<MaintenanceLog> findByVehicleIdAndStatus(String vehicleId, String status);
    List<MaintenanceLog> findByVehicleId(String vehicleId);
}