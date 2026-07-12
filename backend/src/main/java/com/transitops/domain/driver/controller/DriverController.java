package com.transitops.domain.driver.controller;

import com.transitops.domain.driver.dto.DriverPatchRequest;
import com.transitops.domain.driver.dto.DriverRequest;
import com.transitops.domain.driver.dto.DriverResponse;
import com.transitops.domain.driver.service.DriverService;
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
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<DriverResponse>> findAll() {
        return ResponseEntity.ok(driverService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(driverService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DriverResponse> patch(@PathVariable String id, @RequestBody DriverPatchRequest request) {
        return ResponseEntity.ok(driverService.patch(id, request));
    }
}