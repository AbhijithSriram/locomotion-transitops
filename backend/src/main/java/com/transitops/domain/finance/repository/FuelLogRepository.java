package com.transitops.domain.finance.repository;

import com.transitops.domain.finance.entity.FuelLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuelLogRepository extends JpaRepository<FuelLog, String> {
    List<FuelLog> findByVehicleId(String vehicleId);
}
