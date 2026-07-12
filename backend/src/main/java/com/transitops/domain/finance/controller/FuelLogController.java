package com.transitops.domain.finance.controller;

import com.transitops.domain.finance.dto.FuelLogRequest;
import com.transitops.domain.finance.dto.FuelLogResponse;
import com.transitops.domain.finance.service.FuelLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fuel-logs")
@RequiredArgsConstructor
public class FuelLogController {

    private final FuelLogService fuelLogService;

    @PostMapping
    public ResponseEntity<FuelLogResponse> create(@Valid @RequestBody FuelLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fuelLogService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<FuelLogResponse>> findAll(@RequestParam(required = false) String vehicleId) {
        return ResponseEntity.ok(
                vehicleId != null ? fuelLogService.findByVehicle(vehicleId) : fuelLogService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuelLogResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(fuelLogService.findById(id));
    }
}