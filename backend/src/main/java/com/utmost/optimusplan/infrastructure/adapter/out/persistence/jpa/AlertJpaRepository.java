package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.CapacityAlertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlertJpaRepository extends JpaRepository<CapacityAlertJpaEntity, UUID> {

    Optional<CapacityAlertJpaEntity> findByTeamId(UUID teamId);
}
