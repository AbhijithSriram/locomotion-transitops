package com.transitops.domain.maintenance.controller;

import com.transitops.domain.maintenance.dto.MaintenanceCreateRequest;
import com.transitops.domain.maintenance.dto.MaintenanceResponse;
import com.transitops.domain.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    public ResponseEntity<MaintenanceResponse> create(@Valid @RequestBody MaintenanceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceService.create(request));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<MaintenanceResponse> close(@PathVariable String id) {
        return ResponseEntity.ok(maintenanceService.close(id));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>> findAll() {
        return ResponseEntity.ok(maintenanceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(maintenanceService.findById(id));
    }
}