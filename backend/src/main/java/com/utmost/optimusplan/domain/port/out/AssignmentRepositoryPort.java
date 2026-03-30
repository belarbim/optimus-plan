package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.TeamAssignment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepositoryPort {

    TeamAssignment save(TeamAssignment assignment);

    Optional<TeamAssignment> findById(UUID id);

    List<TeamAssignment> findByTeamId(UUID teamId);

    List<TeamAssignment> findByEmployeeId(UUID employeeId);

    /**
     * Returns all assignments for a team that are active (overlap) within the given date range.
     */
    List<TeamAssignment> findActiveByTeamIdAndMonth(UUID teamId, LocalDate from, LocalDate to);

    /**
     * Sums the allocation percentage of all active assignments for the given employee
     * within the date range, optionally excluding one assignment by its ID (used when
     * updating an existing assignment).
     */
    BigDecimal sumActiveAllocationForEmployee(UUID employeeId, LocalDate from, LocalDate to,
                                              UUID excludeId);

    boolean hasActiveAssignmentsByTeamId(UUID teamId);

    boolean hasActiveAssignmentsByEmployeeId(UUID employeeId);

    void deleteById(UUID id);
}
