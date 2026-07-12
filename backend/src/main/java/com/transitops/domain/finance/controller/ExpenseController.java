package com.transitops.domain.finance.controller;

import com.transitops.domain.finance.dto.ExpenseRequest;
import com.transitops.domain.finance.dto.ExpenseResponse;
import com.transitops.domain.finance.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> findAll(@RequestParam(required = false) String vehicleId) {
        return ResponseEntity.ok(
                vehicleId != null ? expenseService.findByVehicle(vehicleId) : expenseService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(expenseService.findById(id));
    }
}