package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.CapacityAlert;
import com.utmost.optimusplan.domain.port.out.AlertRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.CapacityAlertJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.AlertJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.TeamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AlertPersistenceAdapter implements AlertRepositoryPort {

    private final AlertJpaRepository repo;
    private final TeamJpaRepository teamRepo;

    @Override
    public CapacityAlert save(CapacityAlert alert) {
        return toDomain(repo.save(toEntity(alert)));
    }

    @Override
    public Optional<CapacityAlert> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CapacityAlert> findByTeamId(UUID teamId) {
        return repo.findByTeamId(teamId).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return repo.existsById(id);
    }

    private CapacityAlert toDomain(CapacityAlertJpaEntity e) {
        return CapacityAlert.builder()
                .id(e.getId())
                .teamId(e.getTeam().getId())
                .thresholdManDays(e.getThresholdManDays())
                .enabled(e.isEnabled())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private CapacityAlertJpaEntity toEntity(CapacityAlert alert) {
        TeamJpaEntity team = teamRepo.findById(alert.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + alert.getTeamId()));
        CapacityAlertJpaEntity entity = alert.getId() != null
                ? repo.findById(alert.getId()).orElseGet(CapacityAlertJpaEntity::new)
                : new CapacityAlertJpaEntity();
        entity.setId(alert.getId());
        entity.setTeam(team);
        entity.setThresholdManDays(alert.getThresholdManDays());
        entity.setEnabled(alert.isEnabled());
        return entity;
    }
}
