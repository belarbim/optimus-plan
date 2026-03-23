package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "capacity_snapshot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapacitySnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamJpaEntity team;

    @Column(name = "snapshot_month")
    private String snapshotMonth;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "capacity_man_days", precision = 10, scale = 3)
    private BigDecimal capacityManDays;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
