package com.transitops.domain.maintenance.service;

import com.transitops.common.exception.NotFoundException;
import com.transitops.domain.maintenance.dto.MaintenanceCreateRequest;
import com.transitops.domain.maintenance.dto.MaintenanceResponse;
import com.transitops.domain.maintenance.entity.MaintenanceLog;
import com.transitops.domain.maintenance.repository.MaintenanceLogRepository;
import com.transitops.domain.trip.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final DispatchService dispatchService;
    private final MaintenanceLogRepository maintenanceLogRepository;

    @Transactional
    public MaintenanceResponse create(MaintenanceCreateRequest request) {
        MaintenanceLog log = dispatchService.openMaintenance(
                request.vehicleId(), request.type(), request.cost()
        );
        return toResponse(log);
    }

    @Transactional
    public MaintenanceResponse close(String id) {
        MaintenanceLog log = dispatchService.closeMaintenance(id);
        return toResponse(log);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> findAll() {
        return maintenanceLogRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse findById(String id) {
        return toResponse(maintenanceLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Maintenance log not found: " + id)));
    }

    private MaintenanceResponse toResponse(MaintenanceLog log) {
        return new MaintenanceResponse(
                log.getId(), log.getVehicleId(), log.getType(), log.getStatus(),
                log.getCost(), log.getCreatedAt(), log.getClosedAt()
        );
    }
}