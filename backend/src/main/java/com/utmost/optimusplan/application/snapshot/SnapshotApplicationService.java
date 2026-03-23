package com.utmost.optimusplan.application.snapshot;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.CapacitySnapshot;
import com.utmost.optimusplan.domain.port.in.CapacityUseCase;
import com.utmost.optimusplan.domain.port.in.SnapshotUseCase;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.SnapshotRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SnapshotApplicationService implements SnapshotUseCase {

    private static final Logger log = LoggerFactory.getLogger(SnapshotApplicationService.class);

    private final TeamRepositoryPort     teamRepo;
    private final CapacityUseCase        capacityUseCase;
    private final SnapshotRepositoryPort snapshotRepo;
    private final AuditRepositoryPort    auditRepo;

    public SnapshotApplicationService(TeamRepositoryPort teamRepo,
                                       CapacityUseCase capacityUseCase,
                                       SnapshotRepositoryPort snapshotRepo,
                                       AuditRepositoryPort auditRepo) {
        this.teamRepo        = teamRepo;
        this.capacityUseCase = capacityUseCase;
        this.snapshotRepo    = snapshotRepo;
        this.auditRepo       = auditRepo;
    }

    // -------------------------------------------------------------------------
    // SnapshotUseCase
    // -------------------------------------------------------------------------

    @Override
    public List<SnapshotDTO> generateForTeam(UUID teamId, String month) {
        // Validate team exists
        if (!teamRepo.existsById(teamId)) {
            throw new DomainException(new DomainError.NotFound("Team", teamId));
        }

        // Compute current capacity
        CapacityUseCase.CapacityResult result = capacityUseCase.computeCapacity(
                new CapacityUseCase.ComputeCapacityQuery(teamId, month));

        // Replace existing snapshots for this team/month
        snapshotRepo.deleteByTeamIdAndMonth(teamId, month);

        LocalDateTime now = LocalDateTime.now();
        List<CapacitySnapshot> snapshots = result.categoryBreakdown().stream()
                .map(cb -> CapacitySnapshot.builder()
                        .id(UUID.randomUUID())
                        .teamId(teamId)
                        .snapshotMonth(month)
                        .categoryName(cb.categoryName())
                        .capacityManDays(cb.manDays())
                        .createdAt(now)
                        .build())
                .collect(Collectors.toList());

        // Also persist one "total" snapshot when categories are absent
        if (snapshots.isEmpty()) {
            snapshots = List.of(CapacitySnapshot.builder()
                    .id(UUID.randomUUID())
                    .teamId(teamId)
                    .snapshotMonth(month)
                    .categoryName("TOTAL")
                    .capacityManDays(result.totalCapacity())
                    .createdAt(now)
                    .build());
        }

        List<CapacitySnapshot> saved = snapshotRepo.saveAll(snapshots);

        audit("CapacitySnapshot", teamId, "GENERATE_SNAPSHOT",
                Map.of("month", month, "count", String.valueOf(saved.size())));

        return toDto(saved);
    }

    @Override
    public void generateAll(String month) {
        teamRepo.findAll().forEach(team -> {
            try {
                generateForTeam(team.getId(), month);
            } catch (Exception ex) {
                log.error("Failed to generate snapshot for team {} in month {}: {}",
                        team.getId(), month, ex.getMessage(), ex);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<SnapshotDTO> findByTeam(UUID teamId, String from, String to) {
        return toDto(snapshotRepo.findByTeamIdAndMonthRange(teamId, from, to));
    }

    // -------------------------------------------------------------------------
    // Scheduled task – runs on the 1st of every month at 01:00
    // -------------------------------------------------------------------------

    /**
     * Automatically generates snapshots for all teams for the previous calendar month.
     * The cron expression {@code 0 0 1 1 * ?} fires at midnight on the 1st of each month.
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void scheduledMonthlySnapshot() {
        String previousMonth = YearMonth.now().minusMonths(1).toString();
        log.info("Scheduled snapshot generation triggered for month {}", previousMonth);
        generateAll(previousMonth);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<SnapshotDTO> toDto(List<CapacitySnapshot> snapshots) {
        return snapshots.stream()
                .map(s -> new SnapshotDTO(
                        s.getId(),
                        s.getTeamId(),
                        s.getSnapshotMonth(),
                        s.getCategoryName(),
                        s.getCapacityManDays(),
                        s.getCreatedAt()))
                .collect(Collectors.toList());
    }

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
