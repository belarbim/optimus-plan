package com.utmost.optimusplan.application.category;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.CategoryAllocation;
import com.utmost.optimusplan.domain.port.in.CategoryUseCase;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.CategoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryApplicationService implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepo;
    private final TeamRepositoryPort     teamRepo;
    private final AuditRepositoryPort    auditRepo;

    public CategoryApplicationService(CategoryRepositoryPort categoryRepo,
                                       TeamRepositoryPort teamRepo,
                                       AuditRepositoryPort auditRepo) {
        this.categoryRepo = categoryRepo;
        this.teamRepo     = teamRepo;
        this.auditRepo    = auditRepo;
    }

    // -------------------------------------------------------------------------
    // CategoryUseCase
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<CategoryAllocation> findByTeam(UUID teamId) {
        return categoryRepo.findByTeamId(teamId);
    }

    @Override
    public List<CategoryAllocation> setCategories(SetCategoriesCommand cmd) {
        // Ensure the team exists
        if (!teamRepo.existsById(cmd.teamId())) {
            throw new DomainException(new DomainError.NotFound("Team", cmd.teamId()));
        }

        // Validate Incident separately (% of total capacity, 0–100)
        cmd.categories().stream()
                .filter(e -> "Incident".equalsIgnoreCase(e.categoryName()))
                .findFirst()
                .ifPresent(incident -> {
                    if (incident.allocationPct().compareTo(BigDecimal.ZERO) < 0
                            || incident.allocationPct().compareTo(BigDecimal.valueOf(100)) > 0) {
                        throw new DomainException(new DomainError.BusinessRule(
                                "Incident allocation must be between 0 and 100"));
                    }
                });

        // Validate that the 3 planned-work categories (Project, CI, IT for IT)
        // sum to exactly 100 — they split the remaining capacity after Incident.
        BigDecimal plannedTotal = cmd.categories().stream()
                .filter(e -> !"Incident".equalsIgnoreCase(e.categoryName()))
                .map(CategoryEntry::allocationPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (plannedTotal.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Project, Continuous Improvement and IT for IT allocations must sum to 100 (got "
                            + plannedTotal + ")"));
        }

        // Replace existing categories
        categoryRepo.deleteByTeamId(cmd.teamId());

        LocalDateTime now = LocalDateTime.now();
        List<CategoryAllocation> saved = cmd.categories().stream()
                .map(entry -> categoryRepo.save(CategoryAllocation.builder()
                        .id(UUID.randomUUID())
                        .teamId(cmd.teamId())
                        .categoryName(entry.categoryName())
                        .allocationPct(entry.allocationPct())
                        .createdAt(now)
                        .updatedAt(now)
                        .build()))
                .collect(Collectors.toList());

        audit("CategoryAllocation", cmd.teamId(), "SET_CATEGORIES",
                Map.of("teamId", cmd.teamId().toString(),
                       "count", String.valueOf(saved.size())));
        return saved;
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
