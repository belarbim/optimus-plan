package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.RoleHistory;
import com.utmost.optimusplan.domain.port.out.RoleHistoryRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.RoleHistoryJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamAssignmentJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.AssignmentJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.RoleHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleHistoryPersistenceAdapter implements RoleHistoryRepositoryPort {

    private final RoleHistoryJpaRepository repo;
    private final AssignmentJpaRepository assignmentRepo;

    @Override
    public RoleHistory save(RoleHistory roleHistory) {
        return toDomain(repo.save(toEntity(roleHistory)));
    }

    @Override
    public List<RoleHistory> findByAssignmentId(UUID assignmentId) {
        return repo.findByAssignmentId(assignmentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RoleHistory> findCurrentByAssignmentId(UUID assignmentId) {
        return repo.findCurrentByAssignmentId(assignmentId).map(this::toDomain);
    }

    @Override
    public List<RoleHistory> findByAssignmentIdOrdered(UUID assignmentId) {
        return repo.findByAssignmentIdOrderByEffectiveFromAsc(assignmentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void closeCurrentRole(UUID assignmentId, LocalDate effectiveTo) {
        repo.findCurrentByAssignmentId(assignmentId).ifPresent(entity -> {
            entity.setEffectiveTo(effectiveTo);
            repo.save(entity);
        });
    }

    @Override
    public void setLastRoleEndDate(UUID assignmentId, LocalDate effectiveTo) {
        repo.findTopByAssignmentIdOrderByEffectiveFromDesc(assignmentId).ifPresent(entity -> {
            entity.setEffectiveTo(effectiveTo);
            repo.save(entity);
        });
    }

    @Override
    public void updateLastRole(UUID assignmentId, String roleType, java.math.BigDecimal roleWeight, LocalDate effectiveTo) {
        repo.findTopByAssignmentIdOrderByEffectiveFromDesc(assignmentId).ifPresent(entity -> {
            entity.setRoleType(roleType);
            entity.setRoleWeight(roleWeight);
            entity.setEffectiveTo(effectiveTo);
            repo.save(entity);
        });
    }

    @Override
    public void deleteByAssignmentId(UUID assignmentId) {
        repo.deleteByAssignmentId(assignmentId);
    }

    private RoleHistory toDomain(RoleHistoryJpaEntity e) {
        return RoleHistory.builder()
                .id(e.getId())
                .assignmentId(e.getAssignment().getId())
                .roleType(e.getRoleType())
                .roleWeight(e.getRoleWeight())
                .effectiveFrom(e.getEffectiveFrom())
                .effectiveTo(e.getEffectiveTo())
                .build();
    }

    private RoleHistoryJpaEntity toEntity(RoleHistory roleHistory) {
        TeamAssignmentJpaEntity assignment = assignmentRepo.findById(roleHistory.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + roleHistory.getAssignmentId()));
        return RoleHistoryJpaEntity.builder()
                .id(roleHistory.getId())
                .assignment(assignment)
                .roleType(roleHistory.getRoleType())
                .roleWeight(roleHistory.getRoleWeight())
                .effectiveFrom(roleHistory.getEffectiveFrom())
                .effectiveTo(roleHistory.getEffectiveTo())
                .build();
    }
}
