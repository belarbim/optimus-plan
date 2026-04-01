package com.utmost.optimusplan.application.category;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.CategoryAllocation;
import com.utmost.optimusplan.domain.port.in.CategoryUseCase;
import com.utmost.optimusplan.domain.model.TeamType;
import com.utmost.optimusplan.domain.model.TeamTypeCategory;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.CategoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamTypeRepositoryPort;
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
    private final TeamTypeRepositoryPort teamTypeRepo;
    private final AuditRepositoryPort    auditRepo;

    public CategoryApplicationService(CategoryRepositoryPort categoryRepo,
                                       TeamRepositoryPort teamRepo,
                                       TeamTypeRepositoryPort teamTypeRepo,
                                       AuditRepositoryPort auditRepo) {
        this.categoryRepo = categoryRepo;
        this.teamRepo     = teamRepo;
        this.teamTypeRepo = teamTypeRepo;
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
        var team = teamRepo.findById(cmd.teamId())
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("Team", cmd.teamId())));

        // If team has a type, validate categories against the type template
        if (team.getTeamTypeId() != null) {
            TeamType teamType = teamTypeRepo.findById(team.getTeamTypeId())
                    .orElseThrow(() -> new DomainException(
                            new DomainError.NotFound("TeamType", team.getTeamTypeId())));
            validateAgainstTeamType(cmd.categories(), teamType);
        } else {
            // Fallback: no team type — just require all categories sum to 100%
            BigDecimal total = cmd.categories().stream()
                    .map(CategoryEntry::allocationPct)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new DomainException(new DomainError.BusinessRule(
                        "Category allocations must sum to 100 (got " + total + ")"));
            }
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

    private void validateAgainstTeamType(List<CategoryEntry> entries, TeamType teamType) {
        // Build lookup maps for the team type's category definitions
        Map<String, TeamTypeCategory> templateByName = teamType.getCategories().stream()
                .collect(java.util.stream.Collectors.toMap(
                        c -> c.getName().toLowerCase(), c -> c));

        // Every submitted category must exist in the template
        for (CategoryEntry entry : entries) {
            if (!templateByName.containsKey(entry.categoryName().toLowerCase())) {
                throw new DomainException(new DomainError.BusinessRule(
                        "Category '" + entry.categoryName() + "' is not defined in team type '"
                        + teamType.getName() + "'"));
            }
        }

        // Every template category must be submitted
        Map<String, BigDecimal> submitted = entries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.categoryName().toLowerCase(), CategoryEntry::allocationPct));
        for (String templateName : templateByName.keySet()) {
            if (!submitted.containsKey(templateName)) {
                throw new DomainException(new DomainError.BusinessRule(
                        "Missing allocation for category '" + templateByName.get(templateName).getName() + "'"));
            }
        }

        // Each category allocation must be between 0 and 100%
        for (CategoryEntry entry : entries) {
            if (entry.allocationPct().compareTo(BigDecimal.ZERO) < 0
                    || entry.allocationPct().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new DomainException(new DomainError.BusinessRule(
                        "Category '" + entry.categoryName() + "' allocation must be between 0 and 100"));
            }
        }

        boolean hasTotalCats     = teamType.getCategories().stream().anyMatch(TeamTypeCategory::isPartOfTotalCapacity);
        boolean hasRemainingCats = teamType.getCategories().stream().anyMatch(TeamTypeCategory::isPartOfRemainingCapacity);

        BigDecimal totalCatSum = hasTotalCats
                ? teamType.getCategories().stream()
                        .filter(TeamTypeCategory::isPartOfTotalCapacity)
                        .map(c -> submitted.get(c.getName().toLowerCase()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

        // Total-capacity categories must not exceed 100%
        if (totalCatSum.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Total-capacity categories must not exceed 100% (got " + totalCatSum + "%)"));
        }

        if (hasRemainingCats) {
            BigDecimal remainingCatSum = teamType.getCategories().stream()
                    .filter(TeamTypeCategory::isPartOfRemainingCapacity)
                    .map(c -> submitted.get(c.getName().toLowerCase()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean noRemainingCapacity = totalCatSum.compareTo(BigDecimal.valueOf(100)) == 0;
            if (noRemainingCapacity) {
                // Total overhead consumes 100% — no remaining capacity, so remaining categories must all be 0
                if (remainingCatSum.compareTo(BigDecimal.ZERO) != 0) {
                    throw new DomainException(new DomainError.BusinessRule(
                            "Total-capacity categories consume 100% of capacity; remaining categories must all be 0%"));
                }
            } else {
                // There is remaining capacity — remaining categories must sum to 100% of it
                if (remainingCatSum.compareTo(BigDecimal.valueOf(100)) != 0) {
                    throw new DomainException(new DomainError.BusinessRule(
                            "Remaining-capacity categories must sum to exactly 100% of the remaining capacity (got " + remainingCatSum + "%)"));
                }
            }
        }
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
