package com.transitops.domain.maintenance.repository;

import com.transitops.domain.maintenance.entity.MaintenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, String> {
}
