package com.utmost.optimusplan.application.alert;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.CapacityAlert;
import com.utmost.optimusplan.domain.port.in.AlertUseCase;
import com.utmost.optimusplan.domain.port.out.AlertRepositoryPort;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AlertApplicationService implements AlertUseCase {

    private final AlertRepositoryPort alertRepo;
    private final TeamRepositoryPort  teamRepo;
    private final AuditRepositoryPort auditRepo;

    public AlertApplicationService(AlertRepositoryPort alertRepo,
                                    TeamRepositoryPort teamRepo,
                                    AuditRepositoryPort auditRepo) {
        this.alertRepo = alertRepo;
        this.teamRepo  = teamRepo;
        this.auditRepo = auditRepo;
    }

    // -------------------------------------------------------------------------
    // AlertUseCase
    // -------------------------------------------------------------------------

    @Override
    public CapacityAlert createOrUpdate(CreateAlertCommand cmd) {
        // Validate team exists
        if (!teamRepo.existsById(cmd.teamId())) {
            throw new DomainException(new DomainError.NotFound("Team", cmd.teamId()));
        }

        LocalDateTime now = LocalDateTime.now();

        CapacityAlert alert = alertRepo.findByTeamId(cmd.teamId())
                .map(existing -> {
                    existing.setThresholdManDays(cmd.thresholdManDays());
                    existing.setEnabled(cmd.enabled());
                    existing.setUpdatedAt(now);
                    return existing;
                })
                .orElse(CapacityAlert.builder()
                        .id(UUID.randomUUID())
                        .teamId(cmd.teamId())
                        .thresholdManDays(cmd.thresholdManDays())
                        .enabled(cmd.enabled())
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        CapacityAlert saved = alertRepo.save(alert);
        audit("CapacityAlert", saved.getId(), "UPSERT",
                Map.of("teamId", cmd.teamId().toString(),
                       "threshold", cmd.thresholdManDays().toString(),
                       "enabled", String.valueOf(cmd.enabled())));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public CapacityAlert findByTeam(UUID teamId) {
        return alertRepo.findByTeamId(teamId)
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("CapacityAlert", teamId)));
    }

    @Override
    public void delete(UUID id) {
        CapacityAlert alert = alertRepo.findById(id)
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("CapacityAlert", id)));

        alertRepo.deleteById(id);
        audit("CapacityAlert", id, "DELETE",
                Map.of("teamId", alert.getTeamId().toString()));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void audit(String entityType, UUID id, String action, Map<String, Object> changes) {
        auditRepo.save(AuditLog.builder()
                .id(UUID.randomUUID())
                .entityType(entityType)
                .entityId(id)
                .action(action)
                .changes(changes)
                .actor("manager")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
