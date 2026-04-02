package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TeamTypeJpaRepository extends JpaRepository<TeamTypeJpaEntity, UUID> {
    boolean existsByName(String name);
    Optional<TeamTypeJpaEntity> findByNameIgnoreCase(String name);
}
