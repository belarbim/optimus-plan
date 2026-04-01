package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.RoleHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleHistoryRepositoryPort {

    RoleHistory save(RoleHistory roleHistory);

    List<RoleHistory> findByAssignmentId(UUID assignmentId);

    /**
     * Returns the currently active role segment (where effectiveTo IS NULL).
     */
    Optional<RoleHistory> findCurrentByAssignmentId(UUID assignmentId);

    /**
     * Returns all role history entries for an assignment ordered by effectiveFrom ASC.
     */
    List<RoleHistory> findByAssignmentIdOrdered(UUID assignmentId);

    /**
     * Sets effectiveTo on the currently open role segment for the given assignment.
     */
    void closeCurrentRole(UUID assignmentId, LocalDate effectiveTo);

    /**
     * Sets effectiveTo on the most recent role segment regardless of whether it is open or already closed.
     * Used when editing the end date of an assignment.
     */
    void setLastRoleEndDate(UUID assignmentId, LocalDate effectiveTo);

    /**
     * Updates the roleType, roleWeight, and effectiveTo on the most recent role segment in place.
     * Used when editing an assignment directly (no new history segment created).
     */
    void updateLastRole(UUID assignmentId, String roleType, java.math.BigDecimal roleWeight, LocalDate effectiveTo);

    void deleteByAssignmentId(UUID assignmentId);
}
