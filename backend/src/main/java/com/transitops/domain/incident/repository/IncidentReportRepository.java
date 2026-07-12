package com.transitops.domain.incident.repository;

import com.transitops.domain.incident.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, String> {
}