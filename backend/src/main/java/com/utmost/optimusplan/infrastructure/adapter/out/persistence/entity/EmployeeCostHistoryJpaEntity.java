package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "employee_cost_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeCostHistoryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @Column(name = "daily_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyCost;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
