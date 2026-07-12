package com.transitops.domain.finance.entity;

import com.transitops.common.entity.AssignedIdEntity;
import com.transitops.common.enums.ExpenseCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Expense extends AssignedIdEntity {
    private String vehicleId;
    private String tripId;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    private double amount;
    private String description;
    private Instant incurredAt;
}