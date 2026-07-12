package com.transitops.domain.finance.service;

import com.transitops.common.exception.NotFoundException;
import com.transitops.common.util.Ids;
import com.transitops.domain.finance.dto.FuelLogRequest;
import com.transitops.domain.finance.dto.FuelLogResponse;
import com.transitops.domain.finance.entity.FuelLog;
import com.transitops.domain.finance.repository.FuelLogRepository;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FuelLogService {

    private final FuelLogRepository fuelLogRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public FuelLogResponse create(FuelLogRequest request) {
        if (!vehicleRepository.existsById(request.vehicleId())) {
            throw new NotFoundException("Vehicle not found: " + request.vehicleId());
        }

        Instant now = Instant.now();

        FuelLog log = FuelLog.builder()
                .id(Ids.newId())
                .vehicleId(request.vehicleId())
                .tripId(request.tripId())
                .liters(request.liters())
                .cost(request.cost())
                .odometer(request.odometer() != null ? request.odometer() : 0)
                .date(now)
                .loggedAt(now)
                .build();

        return toResponse(fuelLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<FuelLogResponse> findAll() {
        return fuelLogRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuelLogResponse> findByVehicle(String vehicleId) {
        return fuelLogRepository.findByVehicleId(vehicleId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FuelLogResponse findById(String id) {
        return toResponse(fuelLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fuel log not found: " + id)));
    }

    private FuelLogResponse toResponse(FuelLog f) {
        return new FuelLogResponse(
                f.getId(), f.getVehicleId(), f.getTripId(), f.getLiters(),
                f.getCost(), f.getOdometer(), f.getDate(), f.getLoggedAt()
        );
    }
}