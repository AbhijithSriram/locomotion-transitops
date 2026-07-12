package com.transitops.domain.vehicle.controller;

import com.transitops.domain.vehicle.dto.VehiclePatchRequest;
import com.transitops.domain.vehicle.dto.VehicleRequest;
import com.transitops.domain.vehicle.dto.VehicleResponse;
import com.transitops.domain.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> findAll() {
        return ResponseEntity.ok(vehicleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(vehicleService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VehicleResponse> patch(@PathVariable String id, @RequestBody VehiclePatchRequest request) {
        return ResponseEntity.ok(vehicleService.patch(id, request));
    }
}