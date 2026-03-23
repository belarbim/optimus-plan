package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.RoleTypeConfig;
import com.utmost.optimusplan.domain.port.out.RoleTypeRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.RoleTypeConfigJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.RoleTypeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleTypePersistenceAdapter implements RoleTypeRepositoryPort {

    private final RoleTypeJpaRepository repo;

    @Override
    public RoleTypeConfig save(RoleTypeConfig roleTypeConfig) {
        return toDomain(repo.save(toEntity(roleTypeConfig)));
    }

    @Override
    public Optional<RoleTypeConfig> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<RoleTypeConfig> findAll() {
        return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<RoleTypeConfig> findByRoleType(String roleType) {
        return repo.findByRoleType(roleType).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByRoleType(String roleType) {
        return repo.existsByRoleType(roleType);
    }

    @Override
    public boolean isReferencedInHistory(UUID id) {
        return repo.isReferencedInHistory(id);
    }

    private RoleTypeConfig toDomain(RoleTypeConfigJpaEntity e) {
        return RoleTypeConfig.builder()
                .id(e.getId())
                .roleType(e.getRoleType())
                .defaultWeight(e.getDefaultWeight())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private RoleTypeConfigJpaEntity toEntity(RoleTypeConfig config) {
        RoleTypeConfigJpaEntity entity = config.getId() != null
                ? repo.findById(config.getId()).orElseGet(RoleTypeConfigJpaEntity::new)
                : new RoleTypeConfigJpaEntity();
        entity.setId(config.getId());
        entity.setRoleType(config.getRoleType());
        entity.setDefaultWeight(config.getDefaultWeight());
        entity.setDescription(config.getDescription());
        return entity;
    }
}
