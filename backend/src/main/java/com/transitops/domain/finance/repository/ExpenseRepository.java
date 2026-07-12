package com.transitops.domain.finance.repository;

import com.transitops.domain.finance.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, String> {
    List<Expense> findByVehicleId(String vehicleId);
}