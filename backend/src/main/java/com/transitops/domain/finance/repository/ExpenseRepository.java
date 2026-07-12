package com.transitops.domain.finance.repository;

import com.transitops.domain.finance.entity.Expense;
import com.transitops.common.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {
    List<Expense> findByVehicleId(String vehicleId);
    List<Expense> findByCategory(ExpenseCategory category);
}
