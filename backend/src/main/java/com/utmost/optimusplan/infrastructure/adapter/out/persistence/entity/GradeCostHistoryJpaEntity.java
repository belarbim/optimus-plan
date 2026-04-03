package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "grade_cost_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradeCostHistoryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private GradeJpaEntity grade;

    @Column(name = "daily_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyCost;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
