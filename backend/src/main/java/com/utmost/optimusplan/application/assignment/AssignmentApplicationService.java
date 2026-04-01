package com.utmost.optimusplan.application.assignment;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.RoleHistory;
import com.utmost.optimusplan.domain.model.TeamAssignment;
import com.utmost.optimusplan.domain.port.in.AssignmentUseCase;
import com.utmost.optimusplan.domain.port.out.AssignmentRepositoryPort;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.EmployeeRepositoryPort;
import com.utmost.optimusplan.domain.port.out.RoleHistoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AssignmentApplicationService implements AssignmentUseCase {

    private final AssignmentRepositoryPort   assignmentRepo;
    private final RoleHistoryRepositoryPort  roleHistoryRepo;
    private final TeamRepositoryPort         teamRepo;
    private final EmployeeRepositoryPort     employeeRepo;
    private final AuditRepositoryPort        auditRepo;

    public AssignmentApplicationService(AssignmentRepositoryPort assignmentRepo,
                                         RoleHistoryRepositoryPort roleHistoryRepo,
                                         TeamRepositoryPort teamRepo,
                                         EmployeeRepositoryPort employeeRepo,
                                         AuditRepositoryPort auditRepo) {
        this.assignmentRepo  = assignmentRepo;
        this.roleHistoryRepo = roleHistoryRepo;
        this.teamRepo        = teamRepo;
        this.employeeRepo    = employeeRepo;
        this.auditRepo       = auditRepo;
    }

    // -------------------------------------------------------------------------
    // AssignmentUseCase
    // -------------------------------------------------------------------------

    @Override
    public TeamAssignment assign(CreateAssignmentCommand cmd) {
        // Validate team exists
        var team = teamRepo.findById(cmd.teamId())
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("Team", cmd.teamId())));

        // Validate employee exists
        var employee = employeeRepo.findById(cmd.employeeId())
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("Employee", cmd.employeeId())));

        // Check total allocation for employee does not exceed 100 %
        // We query from startDate to a far-future date so we catch all open assignments
        LocalDate from = cmd.startDate();
        LocalDate to   = LocalDate.of(9999, 12, 31);
        BigDecimal existing = assignmentRepo.sumActiveAllocationForEmployee(
                cmd.employeeId(), from, to, null);
        if (existing == null) existing = BigDecimal.ZERO;

        if (existing.add(cmd.allocationPct()).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Total allocation for employee would exceed 100 %"));
        }

        LocalDateTime now = LocalDateTime.now();
        TeamAssignment assignment = TeamAssignment.builder()
                .id(UUID.randomUUID())
                .teamId(cmd.teamId())
                .teamName(team.getName())
                .employeeId(cmd.employeeId())
                .employeeName(employee.fullName())
                .allocationPct(cmd.allocationPct())
                .roleType(cmd.roleType())
                .roleWeight(cmd.roleWeight())
                .startDate(cmd.startDate())
                .endDate(cmd.endDate())
                .createdAt(now)
                .updatedAt(now)
                .build();

        TeamAssignment saved = assignmentRepo.save(assignment);

        // Create the initial role history entry (closed if endDate provided)
        RoleHistory initialHistory = RoleHistory.builder()
                .id(UUID.randomUUID())
                .assignmentId(saved.getId())
                .roleType(cmd.roleType())
                .roleWeight(cmd.roleWeight())
                .effectiveFrom(cmd.startDate())
                .effectiveTo(cmd.endDate())
                .createdAt(now)
                .build();
        roleHistoryRepo.save(initialHistory);

        audit("TeamAssignment", saved.getId(), "CREATE",
                Map.of("teamId", cmd.teamId().toString(),
                       "employeeId", cmd.employeeId().toString(),
                       "allocationPct", cmd.allocationPct().toString()));
        return saved;
    }

    @Override
    public TeamAssignment endAssignment(EndAssignmentCommand cmd) {
        TeamAssignment assignment = getAssignment(cmd.assignmentId());

        if (cmd.endDate().isBefore(assignment.getStartDate())) {
            throw new DomainException(new DomainError.BusinessRule(
                    "End date cannot be before the assignment start date"));
        }

        assignment.setEndDate(cmd.endDate());
        assignment.setUpdatedAt(LocalDateTime.now());

        // Update the end date on the most recent role history segment (open or closed)
        roleHistoryRepo.setLastRoleEndDate(assignment.getId(), cmd.endDate());

        TeamAssignment saved = assignmentRepo.save(assignment);
        audit("TeamAssignment", saved.getId(), "END",
                Map.of("endDate", cmd.endDate().toString()));
        return saved;
    }

    @Override
    public TeamAssignment updateAllocation(UpdateAllocationCommand cmd) {
        TeamAssignment assignment = getAssignment(cmd.assignmentId());

        if (!assignment.isActive()) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Cannot update allocation on an ended assignment"));
        }

        // Re-compute total excluding this assignment
        LocalDate from = assignment.getStartDate();
        LocalDate to   = LocalDate.of(9999, 12, 31);
        BigDecimal others = assignmentRepo.sumActiveAllocationForEmployee(
                assignment.getEmployeeId(), from, to, assignment.getId());
        if (others == null) others = BigDecimal.ZERO;

        if (others.add(cmd.allocationPct()).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Total allocation for employee would exceed 100 %"));
        }

        assignment.setAllocationPct(cmd.allocationPct());
        assignment.setUpdatedAt(LocalDateTime.now());

        TeamAssignment saved = assignmentRepo.save(assignment);
        audit("TeamAssignment", saved.getId(), "UPDATE_ALLOCATION",
                Map.of("allocationPct", cmd.allocationPct().toString()));
        return saved;
    }

    @Override
    public RoleHistory changeRole(ChangeRoleCommand cmd) {
        TeamAssignment assignment = getAssignment(cmd.assignmentId());

        if (!assignment.isActive()) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Cannot change role on an ended assignment"));
        }

        // Validate ordering: new effectiveFrom must be after the current segment's start
        RoleHistory current = roleHistoryRepo.findCurrentByAssignmentId(assignment.getId())
                .orElseThrow(() -> new DomainException(new DomainError.BusinessRule(
                        "No active role history found for assignment")));

        if (!cmd.effectiveFrom().isAfter(current.getEffectiveFrom())) {
            throw new DomainException(new DomainError.BusinessRule(
                    "New role effective date must be after current role's effective date"));
        }

        // Close the current segment
        LocalDate closingDate = cmd.effectiveFrom().minusDays(1);
        roleHistoryRepo.closeCurrentRole(assignment.getId(), closingDate);

        // Create the new role segment
        LocalDateTime now = LocalDateTime.now();
        RoleHistory newHistory = RoleHistory.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignment.getId())
                .roleType(cmd.roleType())
                .roleWeight(cmd.roleWeight())
                .effectiveFrom(cmd.effectiveFrom())
                .effectiveTo(null)
                .createdAt(now)
                .build();

        RoleHistory savedHistory = roleHistoryRepo.save(newHistory);

        // Keep the assignment's snapshot values in sync
        assignment.setRoleType(cmd.roleType());
        assignment.setRoleWeight(cmd.roleWeight());
        assignment.setUpdatedAt(now);
        assignmentRepo.save(assignment);

        audit("RoleHistory", savedHistory.getId(), "CHANGE_ROLE",
                Map.of("assignmentId", assignment.getId().toString(),
                       "roleType", cmd.roleType(),
                       "effectiveFrom", cmd.effectiveFrom().toString()));
        return savedHistory;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamAssignment> findByTeam(UUID teamId) {
        return assignmentRepo.findByTeamId(teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamAssignment> findByEmployee(UUID employeeId) {
        return assignmentRepo.findByEmployeeId(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleHistory> getRoleHistory(UUID assignmentId) {
        return roleHistoryRepo.findByAssignmentIdOrdered(assignmentId);
    }

    @Override
    public TeamAssignment updateAssignment(UpdateAssignmentCommand cmd) {
        TeamAssignment assignment = getAssignment(cmd.assignmentId());

        var team = teamRepo.findById(cmd.teamId())
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("Team", cmd.teamId())));

        // Re-compute total allocation excluding this assignment
        LocalDate from = cmd.startDate() != null ? cmd.startDate() : assignment.getStartDate();
        LocalDate to   = LocalDate.of(9999, 12, 31);
        BigDecimal others = assignmentRepo.sumActiveAllocationForEmployee(
                assignment.getEmployeeId(), from, to, assignment.getId());
        if (others == null) others = BigDecimal.ZERO;

        if (others.add(cmd.allocationPct()).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Total allocation for employee would exceed 100 %"));
        }

        LocalDateTime now = LocalDateTime.now();
        assignment.setTeamId(cmd.teamId());
        assignment.setTeamName(team.getName());
        assignment.setAllocationPct(cmd.allocationPct());
        if (cmd.startDate() != null) assignment.setStartDate(cmd.startDate());
        assignment.setEndDate(cmd.endDate());
        assignment.setRoleType(cmd.roleType());
        assignment.setRoleWeight(cmd.roleWeight());
        assignment.setUpdatedAt(now);

        // Update the most recent role history entry in-place (no new segment)
        roleHistoryRepo.updateLastRole(assignment.getId(), cmd.roleType(), cmd.roleWeight(), cmd.endDate());

        TeamAssignment saved = assignmentRepo.save(assignment);
        audit("TeamAssignment", saved.getId(), "UPDATE",
                Map.of("allocationPct", cmd.allocationPct().toString(),
                       "roleType", cmd.roleType()));
        return saved;
    }

    @Override
    public void deleteAssignment(UUID assignmentId) {
        getAssignment(assignmentId); // throws NotFound if missing
        roleHistoryRepo.deleteByAssignmentId(assignmentId);
        assignmentRepo.deleteById(assignmentId);
        audit("TeamAssignment", assignmentId, "DELETE", Map.of());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private TeamAssignment getAssignment(UUID id) {
        return assignmentRepo.findById(id)
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("TeamAssignment", id)));
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
