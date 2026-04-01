package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "team_type_category")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamTypeCategoryJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_type_id", nullable = false)
    private TeamTypeJpaEntity teamType;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isPartOfTotalCapacity;

    @Column(nullable = false)
    private boolean isPartOfRemainingCapacity;
}
