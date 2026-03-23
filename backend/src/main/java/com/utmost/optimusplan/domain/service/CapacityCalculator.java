package com.utmost.optimusplan.domain.service;

import com.utmost.optimusplan.domain.model.CategoryAllocation;
import com.utmost.optimusplan.domain.model.PublicHoliday;
import com.utmost.optimusplan.domain.model.RoleHistory;
import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.model.TeamAssignment;
import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import com.utmost.optimusplan.domain.port.in.CapacityUseCase.CapacityResult;
import com.utmost.optimusplan.domain.port.in.CapacityUseCase.CategoryBreakdown;
import com.utmost.optimusplan.domain.port.in.CapacityUseCase.EmployeeContribution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pure domain service – no Spring dependencies.
 *
 * <p>Computes man-day capacity for a team in a given month by weighting each
 * employee's contribution by their allocation percentage, blended role weight,
 * and the fraction of the month they were actually active.
 */
public class CapacityCalculator {

    private static final int SCALE = 6;

    private CapacityCalculator() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Computes the full {@link CapacityResult} for a team in a given month.
     *
     * @param team              domain team object
     * @param month             month string in yyyy-MM format
     * @param assignments       assignments active (or overlapping) in the month
     * @param categories        category allocations for the team
     * @param allRoleHistories  role-history entries for all of the above assignments
     * @param holidays          public holidays applicable to the period
     * @param workingDaysConfig optional override for average working days
     */
    public static CapacityResult compute(
            Team team,
            String month,
            List<TeamAssignment> assignments,
            List<CategoryAllocation> categories,
            List<RoleHistory> allRoleHistories,
            List<PublicHoliday> holidays,
            Optional<WorkingDaysConfig> workingDaysConfig) {

        YearMonth ym        = YearMonth.parse(month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd   = ym.atEndOfMonth();

        BigDecimal avgDays = BusinessDayCalculator.getWorkingDays(ym, holidays, workingDaysConfig);
        int totalBusinessDays = BusinessDayCalculator.countBusinessDays(monthStart, monthEnd, holidays);

        List<EmployeeContribution> contributions = buildContributions(
                assignments, allRoleHistories, monthStart, monthEnd,
                totalBusinessDays, avgDays, holidays);

        BigDecimal totalCapacity = contributions.stream()
                .map(EmployeeContribution::totalManDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        List<CategoryBreakdown> breakdown = buildBreakdown(categories, totalCapacity);

        return new CapacityResult(
                team.getId(),
                team.getName(),
                month,
                totalCapacity,
                breakdown,
                contributions);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static List<EmployeeContribution> buildContributions(
            List<TeamAssignment> assignments,
            List<RoleHistory> allRoleHistories,
            LocalDate monthStart,
            LocalDate monthEnd,
            int totalBusinessDays,
            BigDecimal avgDays,
            List<PublicHoliday> holidays) {

        List<EmployeeContribution> contributions = new ArrayList<>();

        for (TeamAssignment assignment : assignments) {

            // Collect and sort role history for this assignment
            List<RoleHistory> history = allRoleHistories.stream()
                    .filter(rh -> rh.getAssignmentId().equals(assignment.getId()))
                    .sorted(Comparator.comparing(RoleHistory::getEffectiveFrom))
                    .collect(Collectors.toList());

            BigDecimal blendedWeight = computeBlendedRoleWeight(
                    history, monthStart, monthEnd, totalBusinessDays, holidays);

            // Clamp the assignment's active period to the month boundaries
            LocalDate effectiveStart = assignment.getStartDate().isBefore(monthStart)
                    ? monthStart : assignment.getStartDate();
            LocalDate effectiveEnd   = (assignment.getEndDate() == null
                    || assignment.getEndDate().isAfter(monthEnd))
                    ? monthEnd : assignment.getEndDate();

            if (effectiveStart.isAfter(effectiveEnd)) {
                continue; // assignment does not overlap this month
            }

            int activeDays = BusinessDayCalculator.countBusinessDays(
                    effectiveStart, effectiveEnd, holidays);

            BigDecimal presenceFactor = totalBusinessDays == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(activeDays)
                    .divide(BigDecimal.valueOf(totalBusinessDays), SCALE, RoundingMode.HALF_UP);

            BigDecimal allocFraction = assignment.getAllocationPct()
                    .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);

            BigDecimal employeeTotal = avgDays
                    .multiply(allocFraction)
                    .multiply(blendedWeight)
                    .multiply(presenceFactor)
                    .setScale(3, RoundingMode.HALF_UP);

            // Derive the "current" role for display purposes
            String      currentRole   = history.isEmpty()
                    ? assignment.getRoleType()   : history.getLast().getRoleType();
            BigDecimal  currentWeight = history.isEmpty()
                    ? assignment.getRoleWeight() : history.getLast().getRoleWeight();

            contributions.add(new EmployeeContribution(
                    assignment.getEmployeeId(),
                    assignment.getEmployeeName(),
                    assignment.getAllocationPct(),
                    currentRole,
                    currentWeight,
                    presenceFactor.setScale(3, RoundingMode.HALF_UP),
                    employeeTotal));
        }

        return contributions;
    }

    /**
     * Breakdown formula:
     * <ol>
     *   <li>Incident man-days  = totalCapacity × incidentPct / 100</li>
     *   <li>Remaining capacity = totalCapacity − incidentManDays</li>
     *   <li>Project / CI / IT4IT man-days = remainingCapacity × categoryPct / 100
     *       (their pcts are each expressed as % of the remaining pool)</li>
     * </ol>
     */
    private static final String INCIDENT = "Incident";

    private static List<CategoryBreakdown> buildBreakdown(
            List<CategoryAllocation> categories, BigDecimal totalCapacity) {

        BigDecimal incidentPct = categories.stream()
                .filter(c -> INCIDENT.equalsIgnoreCase(c.getCategoryName()))
                .map(CategoryAllocation::getAllocationPct)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal incidentManDays = totalCapacity
                .multiply(incidentPct.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP))
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal remainingCapacity = totalCapacity.subtract(incidentManDays)
                .max(BigDecimal.ZERO);

        return categories.stream()
                .map(cat -> {
                    if (INCIDENT.equalsIgnoreCase(cat.getCategoryName())) {
                        return new CategoryBreakdown(cat.getCategoryName(), incidentManDays);
                    }
                    BigDecimal fraction = cat.getAllocationPct()
                            .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
                    return new CategoryBreakdown(
                            cat.getCategoryName(),
                            remainingCapacity.multiply(fraction).setScale(3, RoundingMode.HALF_UP));
                })
                .collect(Collectors.toList());
    }

    /**
     * Time-weighted average role weight across all role-history segments that
     * overlap the month, weighted by the number of business days in each segment.
     */
    private static BigDecimal computeBlendedRoleWeight(
            List<RoleHistory> history,
            LocalDate monthStart,
            LocalDate monthEnd,
            int totalBusinessDays,
            List<PublicHoliday> holidays) {

        if (history.isEmpty()) {
            return BigDecimal.ONE; // treat as full weight when history is absent
        }
        if (totalBusinessDays == 0) {
            return history.getLast().getRoleWeight();
        }

        BigDecimal weighted = BigDecimal.ZERO;

        for (int i = 0; i < history.size(); i++) {
            RoleHistory rh = history.get(i);

            // Clamp segment start to the month
            LocalDate segStart = rh.getEffectiveFrom().isBefore(monthStart)
                    ? monthStart : rh.getEffectiveFrom();

            // Segment end: use effectiveTo clamped to monthEnd, then narrow to next
            // segment's start when segments are adjacent
            LocalDate segEnd = (rh.getEffectiveTo() != null
                    && rh.getEffectiveTo().isBefore(monthEnd))
                    ? rh.getEffectiveTo() : monthEnd;

            if (i + 1 < history.size()) {
                LocalDate nextFrom = history.get(i + 1).getEffectiveFrom();
                if (nextFrom.isBefore(segEnd)) {
                    segEnd = nextFrom.minusDays(1);
                }
            }

            if (segStart.isAfter(segEnd)) {
                continue;
            }

            int days = BusinessDayCalculator.countBusinessDays(segStart, segEnd, holidays);
            weighted = weighted.add(rh.getRoleWeight().multiply(BigDecimal.valueOf(days)));
        }

        return weighted.divide(BigDecimal.valueOf(totalBusinessDays), SCALE, RoundingMode.HALF_UP);
    }
}
