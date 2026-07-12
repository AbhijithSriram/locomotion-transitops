package com.transitops.domain.finance.repository;

import com.transitops.domain.finance.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, String> {
}