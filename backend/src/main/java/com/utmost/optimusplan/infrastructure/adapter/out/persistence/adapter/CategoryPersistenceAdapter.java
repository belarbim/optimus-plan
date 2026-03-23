package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.CategoryAllocation;
import com.utmost.optimusplan.domain.port.out.CategoryRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.CategoryAllocationJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.CategoryJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.TeamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository repo;
    private final TeamJpaRepository teamRepo;

    @Override
    public CategoryAllocation save(CategoryAllocation category) {
        return toDomain(repo.save(toEntity(category)));
    }

    @Override
    public List<CategoryAllocation> findByTeamId(UUID teamId) {
        return repo.findByTeamId(teamId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByTeamId(UUID teamId) {
        repo.deleteByTeamId(teamId);
    }

    @Override
    public BigDecimal sumAllocationByTeamId(UUID teamId) {
        BigDecimal result = repo.sumAllocationByTeamId(teamId);
        return result != null ? result : BigDecimal.ZERO;
    }

    private CategoryAllocation toDomain(CategoryAllocationJpaEntity e) {
        return CategoryAllocation.builder()
                .id(e.getId())
                .teamId(e.getTeam().getId())
                .categoryName(e.getCategoryName())
                .allocationPct(e.getAllocationPct())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private CategoryAllocationJpaEntity toEntity(CategoryAllocation category) {
        TeamJpaEntity team = teamRepo.findById(category.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + category.getTeamId()));
        return CategoryAllocationJpaEntity.builder()
                .id(category.getId())
                .team(team)
                .categoryName(category.getCategoryName())
                .allocationPct(category.getAllocationPct())
                .build();
    }
}
