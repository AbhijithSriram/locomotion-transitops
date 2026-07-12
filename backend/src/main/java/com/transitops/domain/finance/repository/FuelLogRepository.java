package com.transitops.domain.finance.repository;

import com.transitops.domain.finance.entity.FuelLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelLogRepository extends JpaRepository<FuelLog, String> {
}