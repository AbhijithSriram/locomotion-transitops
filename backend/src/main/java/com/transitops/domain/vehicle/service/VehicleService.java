package com.transitops.domain.vehicle.service;

import com.transitops.common.enums.VehicleStatus;
import com.transitops.common.exception.ConflictException;
import com.transitops.common.exception.NotFoundException;
import com.transitops.common.util.Ids;
import com.transitops.domain.vehicle.dto.VehiclePatchRequest;
import com.transitops.domain.vehicle.dto.VehicleRequest;
import com.transitops.domain.vehicle.dto.VehicleResponse;
import com.transitops.domain.vehicle.entity.Vehicle;
import com.transitops.domain.vehicle.entity.VehicleHealth;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        if (vehicleRepository.existsByRegNumber(request.regNumber())) {
            throw new ConflictException("Vehicle with regNumber " + request.regNumber() + " already exists");
        }

        String id = Ids.newId();

        VehicleHealth health = VehicleHealth.builder()
                .vehicleId(id)
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(id)
                .regNumber(request.regNumber())
                .name(request.name())
                .type(request.type())
                .maxLoadKg(request.maxLoadKg())
                .odometer(0)
                .acquisitionCost(request.acquisitionCost())
                .status(VehicleStatus.AVAILABLE)
                .health(health)
                .transportMode(request.transportMode() != null ? request.transportMode() : "TRUCK")
                .build();

        vehicle.setNew(true);
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> findAll() {
        return vehicleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse findById(String id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public VehicleResponse patch(String id, VehiclePatchRequest request) {
        Vehicle vehicle = getOrThrow(id);

        if (request.name() != null) {
            vehicle.setName(request.name());
        }
        if (request.type() != null) {
            vehicle.setType(request.type());
        }
        if (request.maxLoadKg() != null) {
            vehicle.setMaxLoadKg(request.maxLoadKg());
        }
        if (request.odometer() != null) {
            vehicle.setOdometer(request.odometer());
        }
        if (request.status() != null) {
            vehicle.setStatus(request.status());
        }

        return toResponse(vehicleRepository.save(vehicle));
    }

    private Vehicle getOrThrow(String id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));
    }

    private VehicleResponse toResponse(Vehicle v) {
        VehicleHealth h = v.getHealth();
        return new VehicleResponse(
                v.getId(),
                v.getRegNumber(),
                v.getName(),
                v.getType(),
                v.getMaxLoadKg(),
                v.getOdometer(),
                v.getAcquisitionCost(),
                v.getStatus(),
                v.getTransportMode(),
                h != null ? h.getTyres() : 0,
                h != null ? h.getEngine() : 0,
                h != null ? h.getBrakes() : 0,
                h != null ? h.getRiskScore() : 0,
                h != null ? h.getLastServiceAt() : null
        );
    }
}