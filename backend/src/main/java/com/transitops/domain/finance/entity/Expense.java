package com.transitops.domain.finance.entity;

import com.transitops.common.enums.ExpenseCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    private String id;

    private String vehicleId;

    private String tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    private double amount;

    private String description;

    @Column(nullable = false)
    private Instant incurredAt;
}
