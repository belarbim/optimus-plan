package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "working_days_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingDaysConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String month;

    @Column(name = "avg_days_worked", precision = 5, scale = 2)
    private BigDecimal avgDaysWorked;

    @CreationTimestamp
    @Column(name = "imported_at")
    private LocalDateTime importedAt;
}
