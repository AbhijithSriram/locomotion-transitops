package com.transitops.domain.driver.service;

import com.transitops.common.enums.DriverStatus;
import com.transitops.common.exception.ConflictException;
import com.transitops.common.exception.NotFoundException;
import com.transitops.common.util.Ids;
import com.transitops.domain.driver.dto.DriverPatchRequest;
import com.transitops.domain.driver.dto.DriverRequest;
import com.transitops.domain.driver.dto.DriverResponse;
import com.transitops.domain.driver.entity.Driver;
import com.transitops.domain.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    @Transactional
    public DriverResponse create(DriverRequest request) {
        if (driverRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new ConflictException("Driver with licenseNumber " + request.licenseNumber() + " already exists");
        }

        Driver driver = Driver.builder()
                .id(Ids.newId())
                .name(request.name())
                .licenseNumber(request.licenseNumber())
                .licenseCategory(request.licenseCategory())
                .contact(request.contact())
                .licenseExpiry(request.licenseExpiry())
                .safetyScore(100)
                .status(DriverStatus.AVAILABLE)
                .build();

        return toResponse(driverRepository.save(driver));
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> findAll() {
        return driverRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DriverResponse findById(String id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public DriverResponse patch(String id, DriverPatchRequest request) {
        Driver driver = getOrThrow(id);

        if (request.name() != null) {
            driver.setName(request.name());
        }
        if (request.contact() != null) {
            driver.setContact(request.contact());
        }
        if (request.licenseCategory() != null) {
            driver.setLicenseCategory(request.licenseCategory());
        }
        if (request.licenseExpiry() != null) {
            driver.setLicenseExpiry(request.licenseExpiry());
        }
        if (request.safetyScore() != null) {
            driver.setSafetyScore(request.safetyScore());
        }
        if (request.status() != null) {
            driver.setStatus(request.status());
        }

        return toResponse(driverRepository.save(driver));
    }

    private Driver getOrThrow(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver not found: " + id));
    }

    private DriverResponse toResponse(Driver d) {
        return new DriverResponse(
                d.getId(),
                d.getName(),
                d.getLicenseNumber(),
                d.getLicenseCategory(),
                d.getContact(),
                d.getLicenseExpiry(),
                d.getSafetyScore(),
                d.getStatus()
        );
    }
}