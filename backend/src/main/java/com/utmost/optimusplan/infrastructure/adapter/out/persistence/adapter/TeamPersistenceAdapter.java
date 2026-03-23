package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.TeamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TeamPersistenceAdapter implements TeamRepositoryPort {

    private final TeamJpaRepository repo;

    @Override
    public Team save(Team team) {
        TeamJpaEntity entity = toEntity(team);
        return toDomain(repo.save(entity));
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return repo.findById(id).map(this::toDomainWithChildren);
    }

    @Override
    public List<Team> findAll() {
        return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Team> findRoots() {
        return repo.findByParentIsNull().stream().map(this::toDomainWithChildren).collect(Collectors.toList());
    }

    @Override
    public List<Team> findByParentId(UUID parentId) {
        return repo.findByParentId(parentId).stream().map(this::toDomainWithChildren).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByNameAndParentId(String name, UUID parentId) {
        return repo.existsByNameAndParentId(name, parentId);
    }

    @Override
    public boolean existsByNameAndParentIsNull(String name) {
        return repo.existsByNameAndParentIsNull(name);
    }

    @Override
    public boolean hasChildren(UUID id) {
        return repo.existsByParentId(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return repo.existsById(id);
    }

    private Team toDomain(TeamJpaEntity e) {
        return Team.builder()
                .id(e.getId())
                .name(e.getName())
                .parentId(e.getParent() != null ? e.getParent().getId() : null)
                .children(new ArrayList<>())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private Team toDomainWithChildren(TeamJpaEntity e) {
        Team t = toDomain(e);
        t.setChildren(e.getChildren().stream().map(this::toDomainWithChildren).collect(Collectors.toList()));
        return t;
    }

    private TeamJpaEntity toEntity(Team team) {
        TeamJpaEntity entity = team.getId() != null
                ? repo.findById(team.getId()).orElseGet(TeamJpaEntity::new)
                : new TeamJpaEntity();

        entity.setId(team.getId());
        entity.setName(team.getName());

        if (team.getParentId() != null) {
            entity.setParent(repo.findById(team.getParentId()).orElse(null));
        } else {
            entity.setParent(null);
        }

        return entity;
    }
}
