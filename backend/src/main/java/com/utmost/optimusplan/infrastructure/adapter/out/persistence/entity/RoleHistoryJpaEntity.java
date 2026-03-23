package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "role_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private TeamAssignmentJpaEntity assignment;

    @Column(name = "role_type")
    private String roleType;

    @Column(name = "role_weight", precision = 3, scale = 2)
    private BigDecimal roleWeight;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
