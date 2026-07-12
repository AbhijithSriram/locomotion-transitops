package com.transitops.domain.finance.service;

import com.transitops.common.exception.NotFoundException;
import com.transitops.common.util.Ids;
import com.transitops.domain.finance.dto.ExpenseRequest;
import com.transitops.domain.finance.dto.ExpenseResponse;
import com.transitops.domain.finance.entity.Expense;
import com.transitops.domain.finance.repository.ExpenseRepository;
import com.transitops.domain.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        if (!vehicleRepository.existsById(request.vehicleId())) {
            throw new NotFoundException("Vehicle not found: " + request.vehicleId());
        }

        Expense expense = Expense.builder()
                .id(Ids.newId())
                .vehicleId(request.vehicleId())
                .tripId(request.tripId())
                .category(request.category())
                .amount(request.amount())
                .description(request.description())
                .incurredAt(Instant.now())
                .build();

        return toResponse(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAll() {
        return expenseRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findByVehicle(String vehicleId) {
        return expenseRepository.findByVehicleId(vehicleId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(String id) {
        return toResponse(expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense not found: " + id)));
    }

    private ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(), e.getVehicleId(), e.getTripId(), e.getCategory(),
                e.getAmount(), e.getDescription(), e.getIncurredAt()
        );
    }
}