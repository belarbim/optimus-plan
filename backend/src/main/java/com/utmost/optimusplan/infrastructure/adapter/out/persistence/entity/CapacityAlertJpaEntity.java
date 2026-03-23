package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "capacity_alert")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacityAlertJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", unique = true)
    private TeamJpaEntity team;

    @Column(name = "threshold_man_days", precision = 10, scale = 3)
    private BigDecimal thresholdManDays;

    private boolean enabled;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
