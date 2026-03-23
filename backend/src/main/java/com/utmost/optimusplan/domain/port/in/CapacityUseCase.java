package com.utmost.optimusplan.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CapacityUseCase {

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    record ComputeCapacityQuery(UUID teamId, String month) {}

    record RemainingCapacityQuery(UUID teamId, LocalDate date) {}

    record RollupQuery(UUID teamId, String month) {}

    record SimulatedChange(
            String type,           // ADD, REMOVE, MODIFY
            UUID employeeId,
            String roleType,
            BigDecimal roleWeight,
            BigDecimal allocationPct) {}

    record SimulationQuery(
            UUID teamId,
            String month,
            List<SimulatedChange> changes) {}

    // -------------------------------------------------------------------------
    // Result records
    // -------------------------------------------------------------------------

    record CategoryBreakdown(String categoryName, BigDecimal manDays) {}

    record EmployeeContribution(
            UUID employeeId,
            String employeeName,
            BigDecimal allocationPct,
            String roleType,
            BigDecimal roleWeight,
            BigDecimal presenceFactor,
            BigDecimal totalManDays) {}

    record CapacityResult(
            UUID teamId,
            String teamName,
            String month,
            BigDecimal totalCapacity,
            List<CategoryBreakdown> categoryBreakdown,
            List<EmployeeContribution> employeeContributions) {}

    record RemainingCapacityResult(
            LocalDate date,
            LocalDate adjustedDate,
            int remainingBusinessDays,
            int totalBusinessDays,
            BigDecimal totalRemaining,
            List<CategoryBreakdown> categoryBreakdown) {}

    record RollupResult(
            CapacityResult ownCapacity,
            List<CapacityResult> subTeamCapacities,
            BigDecimal consolidatedTotal) {}

    record SimulationResult(
            CapacityResult baseline,
            CapacityResult simulated,
            Map<String, BigDecimal> deltas,
            List<String> warnings) {}

    // -------------------------------------------------------------------------
    // Operations
    // -------------------------------------------------------------------------

    CapacityResult computeCapacity(ComputeCapacityQuery query);

    RemainingCapacityResult computeRemaining(RemainingCapacityQuery query);

    RollupResult computeRollup(RollupQuery query);

    SimulationResult simulate(SimulationQuery query);
}
