package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.TeamAssignment;
import com.utmost.optimusplan.domain.port.out.AssignmentRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamAssignmentJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.AssignmentJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.EmployeeJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.RoleHistoryJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.TeamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AssignmentPersistenceAdapter implements AssignmentRepositoryPort {

    private final AssignmentJpaRepository repo;
    private final TeamJpaRepository teamRepo;
    private final EmployeeJpaRepository employeeRepo;
    private final RoleHistoryJpaRepository roleHistoryRepo;

    @Override
    public TeamAssignment save(TeamAssignment assignment) {
        return toDomain(repo.save(toEntity(assignment)));
    }

    @Override
    public Optional<TeamAssignment> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<TeamAssignment> findByTeamId(UUID teamId) {
        return repo.findByTeamId(teamId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TeamAssignment> findByEmployeeId(UUID employeeId) {
        return repo.findByEmployeeId(employeeId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TeamAssignment> findActiveByTeamIdAndMonth(UUID teamId, LocalDate from, LocalDate to) {
        return repo.findActiveByTeamIdInRange(teamId, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumActiveAllocationForEmployee(UUID employeeId, LocalDate from, LocalDate to, UUID excludeId) {
        BigDecimal result = repo.sumActiveAllocationForEmployee(employeeId, from, to, excludeId);
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public boolean hasActiveAssignmentsByTeamId(UUID teamId) {
        return repo.hasActiveAssignmentsByTeamId(teamId);
    }

    @Override
    public boolean hasActiveAssignmentsByEmployeeId(UUID employeeId) {
        return repo.hasActiveAssignmentsByEmployeeId(employeeId);
    }

    private TeamAssignment toDomain(TeamAssignmentJpaEntity e) {
        String employeeName = e.getEmployee().getFirstName() + " " + e.getEmployee().getLastName();
        // Resolve current role from role_history (effectiveTo IS NULL)
        var currentRole = roleHistoryRepo.findCurrentByAssignmentId(e.getId());
        String roleType = currentRole.map(rh -> rh.getRoleType()).orElse(null);
        BigDecimal roleWeight = currentRole.map(rh -> rh.getRoleWeight()).orElse(null);
        return TeamAssignment.builder()
                .id(e.getId())
                .teamId(e.getTeam().getId())
                .teamName(e.getTeam().getName())
                .employeeId(e.getEmployee().getId())
                .employeeName(employeeName)
                .allocationPct(e.getAllocationPct())
                .roleType(roleType)
                .roleWeight(roleWeight)
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private TeamAssignmentJpaEntity toEntity(TeamAssignment assignment) {
        TeamJpaEntity team = teamRepo.findById(assignment.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + assignment.getTeamId()));
        EmployeeJpaEntity employee = employeeRepo.findById(assignment.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + assignment.getEmployeeId()));
        TeamAssignmentJpaEntity entity = assignment.getId() != null
                ? repo.findById(assignment.getId()).orElseGet(TeamAssignmentJpaEntity::new)
                : new TeamAssignmentJpaEntity();
        entity.setId(assignment.getId());
        entity.setTeam(team);
        entity.setEmployee(employee);
        entity.setAllocationPct(assignment.getAllocationPct());
        entity.setStartDate(assignment.getStartDate());
        entity.setEndDate(assignment.getEndDate());
        return entity;
    }
}
