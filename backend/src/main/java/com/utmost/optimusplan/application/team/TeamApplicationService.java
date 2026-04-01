package com.utmost.optimusplan.application.team;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.port.in.TeamUseCase;
import com.utmost.optimusplan.domain.port.out.AssignmentRepositoryPort;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import com.utmost.optimusplan.domain.model.AuditLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TeamApplicationService implements TeamUseCase {

    private final TeamRepositoryPort       teamRepo;
    private final AssignmentRepositoryPort assignmentRepo;
    private final AuditRepositoryPort      auditRepo;

    public TeamApplicationService(TeamRepositoryPort teamRepo,
                                   AssignmentRepositoryPort assignmentRepo,
                                   AuditRepositoryPort auditRepo) {
        this.teamRepo       = teamRepo;
        this.assignmentRepo = assignmentRepo;
        this.auditRepo      = auditRepo;
    }

    // -------------------------------------------------------------------------
    // TeamUseCase
    // -------------------------------------------------------------------------

    @Override
    public Team create(CreateTeamCommand cmd) {
        validateNameUniqueness(cmd.name(), cmd.parentId());

        if (cmd.parentId() != null) {
            Team parent = teamRepo.findById(cmd.parentId())
                    .orElseThrow(() -> new DomainException(
                            new DomainError.NotFound("Team", cmd.parentId())));
            if (parent.getParentId() != null) {
                throw new DomainException(new DomainError.BusinessRule(
                        "A sub-team cannot be used as a parent team"));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Team team = Team.builder()
                .id(UUID.randomUUID())
                .name(cmd.name())
                .parentId(cmd.parentId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Team saved = teamRepo.save(team);
        audit("Team", saved.getId(), "CREATE",
                Map.of("name", saved.getName(),
                       "parentId", String.valueOf(saved.getParentId())));
        return saved;
    }

    @Override
    public Team update(UpdateTeamCommand cmd) {
        Team team = findById(cmd.id());

        if (!team.getName().equals(cmd.name())) {
            validateNameUniqueness(cmd.name(), team.getParentId());
        }

        team.setName(cmd.name());
        team.setUpdatedAt(LocalDateTime.now());

        Team saved = teamRepo.save(team);
        audit("Team", saved.getId(), "UPDATE", Map.of("name", saved.getName()));
        return saved;
    }

    @Override
    public void delete(UUID id) {
        Team team = findById(id);

        if (teamRepo.hasChildren(id)) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Cannot delete a team that has sub-teams"));
        }
        if (assignmentRepo.hasActiveAssignmentsByTeamId(id)) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Cannot delete a team with active assignments"));
        }

        teamRepo.deleteById(id);
        audit("Team", id, "DELETE", Map.of("name", team.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public Team findById(UUID id) {
        return teamRepo.findById(id)
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("Team", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAll(boolean tree) {
        if (tree) {
            // findRoots() is expected to return roots with their children already populated
            return teamRepo.findRoots();
        }
        return teamRepo.findAll();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateNameUniqueness(String name, UUID parentId) {
        boolean duplicate = parentId == null
                ? teamRepo.existsByNameAndParentIsNull(name)
                : teamRepo.existsByNameAndParentId(name, parentId);

        if (duplicate) {
            throw new DomainException(new DomainError.Conflict(
                    "A team with name '" + name + "' already exists at this level"));
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
