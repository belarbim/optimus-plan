package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.RoleHistory;
import com.utmost.optimusplan.domain.model.TeamAssignment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AssignmentUseCase {

    record CreateAssignmentCommand(
            UUID teamId,
            UUID employeeId,
            BigDecimal allocationPct,
            String roleType,
            BigDecimal roleWeight,
            LocalDate startDate) {}

    record EndAssignmentCommand(UUID assignmentId, LocalDate endDate) {}

    record UpdateAllocationCommand(UUID assignmentId, BigDecimal allocationPct) {}

    record ChangeRoleCommand(
            UUID assignmentId,
            String roleType,
            BigDecimal roleWeight,
            LocalDate effectiveFrom) {}

    TeamAssignment assign(CreateAssignmentCommand cmd);

    TeamAssignment endAssignment(EndAssignmentCommand cmd);

    TeamAssignment updateAllocation(UpdateAllocationCommand cmd);

    RoleHistory changeRole(ChangeRoleCommand cmd);

    List<TeamAssignment> findByTeam(UUID teamId);

    List<TeamAssignment> findByEmployee(UUID employeeId);

    List<RoleHistory> getRoleHistory(UUID assignmentId);

    void deleteAssignment(UUID assignmentId);
}
