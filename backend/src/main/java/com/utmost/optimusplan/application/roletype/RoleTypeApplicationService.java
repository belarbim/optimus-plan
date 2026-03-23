package com.utmost.optimusplan.application.roletype;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.RoleTypeConfig;
import com.utmost.optimusplan.domain.port.in.RoleTypeUseCase;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.RoleTypeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class RoleTypeApplicationService implements RoleTypeUseCase {

    private final RoleTypeRepositoryPort roleTypeRepo;
    private final AuditRepositoryPort    auditRepo;

    public RoleTypeApplicationService(RoleTypeRepositoryPort roleTypeRepo,
                                       AuditRepositoryPort auditRepo) {
        this.roleTypeRepo = roleTypeRepo;
        this.auditRepo    = auditRepo;
    }

    // -------------------------------------------------------------------------
    // RoleTypeUseCase
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<RoleTypeConfig> findAll() {
        return roleTypeRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleTypeConfig findById(UUID id) {
        return roleTypeRepo.findById(id)
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("RoleTypeConfig", id)));
    }

    @Override
    public RoleTypeConfig create(CreateRoleTypeCommand cmd) {
        if (roleTypeRepo.existsByRoleType(cmd.roleType())) {
            throw new DomainException(new DomainError.Conflict(
                    "Role type '" + cmd.roleType() + "' already exists"));
        }

        LocalDateTime now = LocalDateTime.now();
        RoleTypeConfig config = RoleTypeConfig.builder()
                .id(UUID.randomUUID())
                .roleType(cmd.roleType())
                .defaultWeight(cmd.defaultWeight())
                .description(cmd.description())
                .createdAt(now)
                .updatedAt(now)
                .build();

        RoleTypeConfig saved = roleTypeRepo.save(config);
        audit("RoleTypeConfig", saved.getId(), "CREATE",
                Map.of("roleType", saved.getRoleType()));
        return saved;
    }

    @Override
    public RoleTypeConfig update(UpdateRoleTypeCommand cmd) {
        RoleTypeConfig config = findById(cmd.id());

        // If the role type code is being changed, check uniqueness
        if (!config.getRoleType().equalsIgnoreCase(cmd.roleType())
                && roleTypeRepo.existsByRoleType(cmd.roleType())) {
            throw new DomainException(new DomainError.Conflict(
                    "Role type '" + cmd.roleType() + "' already exists"));
        }

        config.setRoleType(cmd.roleType());
        config.setDefaultWeight(cmd.defaultWeight());
        config.setDescription(cmd.description());
        config.setUpdatedAt(LocalDateTime.now());

        RoleTypeConfig saved = roleTypeRepo.save(config);
        audit("RoleTypeConfig", saved.getId(), "UPDATE",
                Map.of("roleType", saved.getRoleType()));
        return saved;
    }

    @Override
    public void delete(UUID id) {
        RoleTypeConfig config = findById(id); // throws NotFound if absent

        if (roleTypeRepo.isReferencedInHistory(id)) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Role type is in use and cannot be deleted"));
        }

        roleTypeRepo.deleteById(id);
        audit("RoleTypeConfig", id, "DELETE",
                Map.of("roleType", config.getRoleType()));
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
