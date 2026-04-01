package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.TeamType;
import com.utmost.optimusplan.domain.model.TeamTypeCategory;
import com.utmost.optimusplan.domain.port.out.TeamTypeRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamTypeCategoryJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamTypeJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.TeamTypeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TeamTypePersistenceAdapter implements TeamTypeRepositoryPort {

    private final TeamTypeJpaRepository repo;

    @Override
    public TeamType save(TeamType teamType) {
        TeamTypeJpaEntity entity = repo.findById(teamType.getId())
                .orElseGet(TeamTypeJpaEntity::new);
        entity.setId(teamType.getId());
        entity.setName(teamType.getName());
        entity.getCategories().clear();
        teamType.getCategories().forEach(cat -> {
            TeamTypeCategoryJpaEntity catEntity = new TeamTypeCategoryJpaEntity();
            catEntity.setId(cat.getId());
            catEntity.setTeamType(entity);
            catEntity.setName(cat.getName());
            catEntity.setPartOfTotalCapacity(cat.isPartOfTotalCapacity());
            catEntity.setPartOfRemainingCapacity(cat.isPartOfRemainingCapacity());
            entity.getCategories().add(catEntity);
        });
        return toDomain(repo.save(entity));
    }

    @Override
    public Optional<TeamType> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<TeamType> findAll() {
        return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repo.existsByName(name);
    }

    @Override
    public boolean existsById(UUID id) {
        return repo.existsById(id);
    }

    private TeamType toDomain(TeamTypeJpaEntity e) {
        List<TeamTypeCategory> cats = e.getCategories().stream()
                .map(c -> TeamTypeCategory.builder()
                        .id(c.getId())
                        .teamTypeId(e.getId())
                        .name(c.getName())
                        .isPartOfTotalCapacity(c.isPartOfTotalCapacity())
                        .isPartOfRemainingCapacity(c.isPartOfRemainingCapacity())
                        .build())
                .collect(Collectors.toList());
        return TeamType.builder()
                .id(e.getId())
                .name(e.getName())
                .categories(cats)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
