package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamJpaRepository extends JpaRepository<TeamJpaEntity, UUID> {

    List<TeamJpaEntity> findByParentIsNull();

    List<TeamJpaEntity> findByParentId(UUID parentId);

    boolean existsByNameAndParentId(String name, UUID parentId);

    boolean existsByNameAndParentIsNull(String name);

    boolean existsByParentId(UUID parentId);

    Optional<TeamJpaEntity> findByNameIgnoreCase(String name);
}
