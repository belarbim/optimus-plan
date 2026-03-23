package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamAssignmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AssignmentJpaRepository extends JpaRepository<TeamAssignmentJpaEntity, UUID> {

    List<TeamAssignmentJpaEntity> findByTeamId(UUID teamId);

    List<TeamAssignmentJpaEntity> findByEmployeeId(UUID employeeId);

    @Query("""
            SELECT ta FROM TeamAssignmentJpaEntity ta
            WHERE ta.team.id = :teamId
            AND ta.startDate <= :to
            AND (ta.endDate IS NULL OR ta.endDate >= :from)
            """)
    List<TeamAssignmentJpaEntity> findActiveByTeamIdInRange(
            @Param("teamId") UUID teamId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT COALESCE(SUM(ta.allocationPct), 0)
            FROM TeamAssignmentJpaEntity ta
            WHERE ta.employee.id = :employeeId
            AND ta.startDate <= :to
            AND (ta.endDate IS NULL OR ta.endDate >= :from)
            AND (:excludeId IS NULL OR ta.id <> :excludeId)
            AND (ta.endDate IS NULL OR ta.endDate >= CURRENT_DATE)
            """)
    BigDecimal sumActiveAllocationForEmployee(
            @Param("employeeId") UUID employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(ta) > 0 FROM TeamAssignmentJpaEntity ta WHERE ta.team.id = :teamId AND (ta.endDate IS NULL OR ta.endDate >= CURRENT_DATE)")
    boolean hasActiveAssignmentsByTeamId(@Param("teamId") UUID teamId);

    @Query("SELECT COUNT(ta) > 0 FROM TeamAssignmentJpaEntity ta WHERE ta.employee.id = :employeeId AND (ta.endDate IS NULL OR ta.endDate >= CURRENT_DATE)")
    boolean hasActiveAssignmentsByEmployeeId(@Param("employeeId") UUID employeeId);
}
