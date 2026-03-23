package com.utmost.optimusplan.domain.service;

import com.utmost.optimusplan.domain.model.CategoryAllocation;
import com.utmost.optimusplan.domain.model.PublicHoliday;
import com.utmost.optimusplan.domain.model.RoleHistory;
import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.model.TeamAssignment;
import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import com.utmost.optimusplan.domain.port.in.CapacityUseCase.CapacityResult;
import com.utmost.optimusplan.domain.port.in.CapacityUseCase.CategoryBreakdown;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CapacityCalculator}.
 *
 * <p>Formula:
 * <pre>
 *   employeeTotal = avgDays × allocFraction × blendedWeight × presenceFactor
 *
 *   avgDays       = WorkingDaysConfig.avgDaysWorked  (or countBusinessDays when absent)
 *   allocFraction = allocationPct / 100
 *   blendedWeight = Σ(weight_i × segment_business_days_i) / totalBusinessDays
 *   presenceFactor= activeDays / totalBusinessDays
 * </pre>
 *
 * <p>Reference month: January 2024 (Mon 1 Jan → Wed 31 Jan).
 * Business days: 23 (no holidays).
 */
class CapacityCalculatorTest {

    private static final String MONTH    = "2024-01";
    private static final UUID   TEAM_ID  = UUID.randomUUID();
    private static final BigDecimal AVG_DAYS = new BigDecimal("20");

    // ── empty / zero edge cases ───────────────────────────────────────────────

    @Test
    void compute_noAssignments_returnsZeroCapacity() {
        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(), List.of(), List.of(), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        assertThat(result.totalCapacity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.employeeContributions()).isEmpty();
        assertThat(result.categoryBreakdown()).isEmpty();
    }

    @Test
    void compute_zeroAllocationEmployee_contributesZero() {
        UUID assignId = UUID.randomUUID();
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 0, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE);

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // 20 × 0 × 1.0 × 1.0 = 0
        assertThat(result.totalCapacity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── single employee, full month ───────────────────────────────────────────

    @Test
    void compute_singleFullTimeEmployee_totalEqualsAvgDays() {
        UUID assignId = UUID.randomUUID();
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE);

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // 20 × 1.0 × 1.0 × (23/23) = 20
        assertThat(result.totalCapacity()).isEqualByComparingTo(AVG_DAYS);
        assertThat(result.teamId()).isEqualTo(TEAM_ID);
        assertThat(result.month()).isEqualTo(MONTH);
        assertThat(result.employeeContributions()).hasSize(1);
    }

    @Test
    void compute_halfTimeEmployee_totalIsHalfOfAvgDays() {
        UUID assignId = UUID.randomUUID();
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 50, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE);

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // 20 × 0.5 × 1.0 × 1.0 = 10
        assertThat(result.totalCapacity()).isEqualByComparingTo(new BigDecimal("10.000"));
    }

    @Test
    void compute_employeeWithWeight08_reducesCapacity() {
        UUID assignId = UUID.randomUUID();
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, new BigDecimal("0.8"));

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // 20 × 1.0 × 0.8 × 1.0 = 16
        assertThat(result.totalCapacity()).isEqualByComparingTo(new BigDecimal("16.000"));
    }

    // ── two employees ─────────────────────────────────────────────────────────

    @Test
    void compute_twoEmployees_totalIsSum() {
        UUID a1 = UUID.randomUUID();
        UUID a2 = UUID.randomUUID();
        List<TeamAssignment> assignments = List.of(
                assignment(a1, "2024-01-01", null, 100, null, null),
                assignment(a2, "2024-01-01", null, 100, null, null));
        List<RoleHistory> roles = List.of(
                roleHistory(a1, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE),
                roleHistory(a2, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE));

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH, assignments, List.of(), roles, List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // 20 + 20 = 40
        assertThat(result.totalCapacity()).isEqualByComparingTo(new BigDecimal("40.000"));
        assertThat(result.employeeContributions()).hasSize(2);
    }

    // ── partial month presence ────────────────────────────────────────────────

    @Test
    void compute_assignmentStartsBeforeMonth_clampedToMonthStart() {
        UUID assignId = UUID.randomUUID();
        // Starts Dec 1 but month is Jan → clamped to Jan 1, full presence factor
        TeamAssignment assignment = assignment(assignId, "2023-12-01", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2023, 12, 1), null, BigDecimal.ONE);

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // Presence factor = 23/23 = 1.0 after clamping → same as full month
        assertThat(result.totalCapacity()).isEqualByComparingTo(AVG_DAYS);
    }

    @Test
    void compute_employeeJoinsMidMonth_presenceFactorReducesCapacity() {
        UUID assignId = UUID.randomUUID();
        // Jan 2024: 23 business days total.
        // Jan 16 (Tuesday) → Jan 31: 12 business days (Jan 16,17,18,19,22,23,24,25,26,29,30,31).
        // presenceFactor = 12/23; blendedWeight = (1.0×12)/23 = 12/23
        // employeeTotal = 20 × 1.0 × (12/23) × (12/23) = 20 × 144/529 ≈ 5.444
        TeamAssignment assignment = assignment(assignId, "2024-01-16", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 16), null, BigDecimal.ONE);

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        assertThat(result.totalCapacity()).isLessThan(AVG_DAYS);
        assertThat(result.totalCapacity()).isGreaterThan(BigDecimal.ZERO);
        // Precise bound: 20 × (12/23)^2 = 20 × 0.2718... ≈ 5.436 → rounds to 5.437 or nearby
        assertThat(result.totalCapacity()).isLessThan(new BigDecimal("6"));
    }

    @Test
    void compute_assignmentEndsBeforeMonth_notIncluded() {
        UUID assignId = UUID.randomUUID();
        // Assignment ended Dec 31, 2023 → no overlap with Jan 2024
        TeamAssignment assignment = assignment(assignId, "2023-12-01", "2023-12-31", 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 31), BigDecimal.ONE);

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        assertThat(result.totalCapacity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.employeeContributions()).isEmpty();
    }

    // ── category breakdown ────────────────────────────────────────────────────

    @Test
    void compute_withCategories_incidentDeductedFirstThenSplitRemaining() {
        UUID assignId = UUID.randomUUID();
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE);

        // totalCapacity = 20
        // Incident 20%  → manDays = 20 × 0.20 = 4.000
        // remaining     = 20 − 4 = 16
        // Project 50%   → 16 × 0.50 = 8.000
        // CI 30%        → 16 × 0.30 = 4.800
        // IT4IT 20%     → 16 × 0.20 = 3.200
        // Sum           = 4 + 8 + 4.8 + 3.2 = 20 = totalCapacity ✓
        List<CategoryAllocation> categories = List.of(
                categoryAllocation("Incident", 20),
                categoryAllocation("Project", 50),
                categoryAllocation("Continuous Improvement", 30),
                categoryAllocation("IT for IT", 20));

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), categories, List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        assertThat(findBreakdown(result, "Incident").manDays())
                .isEqualByComparingTo(new BigDecimal("4.000"));
        assertThat(findBreakdown(result, "Project").manDays())
                .isEqualByComparingTo(new BigDecimal("8.000"));
        assertThat(findBreakdown(result, "Continuous Improvement").manDays())
                .isEqualByComparingTo(new BigDecimal("4.800"));
        assertThat(findBreakdown(result, "IT for IT").manDays())
                .isEqualByComparingTo(new BigDecimal("3.200"));

        BigDecimal sumBreakdown = result.categoryBreakdown().stream()
                .map(CategoryBreakdown::manDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumBreakdown).isEqualByComparingTo(result.totalCapacity());
    }

    @Test
    void compute_zeroIncident_plannedCategoriesGetFullCapacity() {
        UUID assignId = UUID.randomUUID();
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE);

        List<CategoryAllocation> categories = List.of(
                categoryAllocation("Incident", 0),
                categoryAllocation("Project", 100));

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), categories, List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // remaining = 20 − 0 = 20 → Project 100% of 20 = 20
        assertThat(findBreakdown(result, "Incident").manDays())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(findBreakdown(result, "Project").manDays())
                .isEqualByComparingTo(result.totalCapacity());
    }

    @Test
    void compute_fullIncident_plannedCategoriesGetZero() {
        UUID assignId = UUID.randomUUID();
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE);

        List<CategoryAllocation> categories = List.of(
                categoryAllocation("Incident", 100),
                categoryAllocation("Project", 100));

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), categories, List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // incident = 20, remaining = 0 → Project gets 0
        assertThat(findBreakdown(result, "Incident").manDays())
                .isEqualByComparingTo(new BigDecimal("20.000"));
        assertThat(findBreakdown(result, "Project").manDays())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── role history blending ─────────────────────────────────────────────────

    @Test
    void compute_twoRoleSegments_blendedWeightIsTimeWeighted() {
        UUID assignId = UUID.randomUUID();
        // Jan 2024: 23 total business days
        // Segment 1: Jan  1–15 (weight 1.0) → 11 business days  (Mon 1 to Mon 15)
        // Segment 2: Jan 16–31 (weight 0.5) → 12 business days  (Tue 16 to Wed 31)
        //
        // blendedWeight = (11 × 1.0 + 12 × 0.5) / 23 = 17 / 23
        // presenceFactor = 23 / 23 = 1.0   (assignment covers full month)
        // employeeTotal  = 20 × 1.0 × (17/23) × 1.0 = 340/23 ≈ 14.783 → 14.783
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100, null, null);
        List<RoleHistory> roles = List.of(
                roleHistory(assignId, LocalDate.of(2024, 1, 1),  LocalDate.of(2024, 1, 15), new BigDecimal("1.0")),
                roleHistory(assignId, LocalDate.of(2024, 1, 16), null,                       new BigDecimal("0.5")));

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), roles, List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // 340/23 ≈ 14.782608... → setScale(3, HALF_UP) = 14.783
        assertThat(result.totalCapacity()).isEqualByComparingTo(new BigDecimal("14.783"));
    }

    // ── no role history fallback ──────────────────────────────────────────────

    @Test
    void compute_noRoleHistory_defaultsToWeight1() {
        UUID assignId = UUID.randomUUID();
        // No RoleHistory entries → blendedWeight defaults to 1.0
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100,
                "Developer", new BigDecimal("1.0"));

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // 20 × 1.0 × 1.0 × 1.0 = 20
        assertThat(result.totalCapacity()).isEqualByComparingTo(AVG_DAYS);
    }

    // ── public holidays ───────────────────────────────────────────────────────

    @Test
    void compute_holidayOnBusinessDay_reducesTotalBusinessDays() {
        UUID assignId = UUID.randomUUID();
        // Jan 2 is a Tuesday (business day) → holiday removes it from totalBusinessDays
        // totalBusinessDays = 22, activeDays = 22
        // blendedWeight  = (1.0 × 22) / 22 = 1.0
        // presenceFactor = 22 / 22 = 1.0
        // employeeTotal  = 20 × 1.0 × 1.0 × 1.0 = 20 (avgDays is from config, unchanged)
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE);
        PublicHoliday holiday = PublicHoliday.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2024, 1, 2))
                .name("New Year observed")
                .locale("FR")
                .recurring(false)
                .build();

        CapacityResult withHoliday = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role),
                List.of(holiday),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        CapacityResult withoutHoliday = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.of(workingDaysConfig(AVG_DAYS)));

        // With WorkingDaysConfig override, avgDays is fixed at 20 regardless of holidays.
        // For a full-month employee: presenceFactor = activeDays/totalBusinessDays cancels out,
        // so totalCapacity stays at 20 whether there is a holiday or not.
        assertThat(withHoliday.totalCapacity()).isEqualByComparingTo(withoutHoliday.totalCapacity());
        assertThat(withHoliday.totalCapacity()).isEqualByComparingTo(AVG_DAYS);
    }

    // ── no working days config ────────────────────────────────────────────────

    @Test
    void compute_noWorkingDaysConfig_avgDaysEqualsActualBusinessDayCount() {
        UUID assignId = UUID.randomUUID();
        TeamAssignment assignment = assignment(assignId, "2024-01-01", null, 100, null, null);
        RoleHistory role = roleHistory(assignId, LocalDate.of(2024, 1, 1), null, BigDecimal.ONE);

        CapacityResult result = CapacityCalculator.compute(
                team(), MONTH,
                List.of(assignment), List.of(), List.of(role), List.of(),
                Optional.empty());

        // avgDays = 23 (actual Jan 2024 business days), presenceFactor = 1.0, weight = 1.0
        // employeeTotal = 23 × 1.0 × 1.0 × 1.0 = 23
        assertThat(result.totalCapacity()).isEqualByComparingTo(new BigDecimal("23.000"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Team team() {
        return Team.builder()
                .id(TEAM_ID).name("Test Team")
                .children(List.of())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private static TeamAssignment assignment(UUID id, String start, String end,
                                              int allocPct, String roleType, BigDecimal roleWeight) {
        return TeamAssignment.builder()
                .id(id)
                .teamId(TEAM_ID).teamName("Test Team")
                .employeeId(UUID.randomUUID()).employeeName("John Doe")
                .allocationPct(BigDecimal.valueOf(allocPct))
                .roleType(roleType).roleWeight(roleWeight)
                .startDate(LocalDate.parse(start))
                .endDate(end != null ? LocalDate.parse(end) : null)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private static RoleHistory roleHistory(UUID assignmentId, LocalDate from, LocalDate to,
                                            BigDecimal weight) {
        return RoleHistory.builder()
                .id(UUID.randomUUID())
                .assignmentId(assignmentId)
                .roleType("Developer").roleWeight(weight)
                .effectiveFrom(from).effectiveTo(to)
                .build();
    }

    private static CategoryAllocation categoryAllocation(String name, int pct) {
        return CategoryAllocation.builder()
                .id(UUID.randomUUID()).teamId(TEAM_ID)
                .categoryName(name)
                .allocationPct(BigDecimal.valueOf(pct))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private static WorkingDaysConfig workingDaysConfig(BigDecimal avgDays) {
        return WorkingDaysConfig.builder()
                .id(UUID.randomUUID())
                .month("2024-01")
                .avgDaysWorked(avgDays)
                .build();
    }

    private static CategoryBreakdown findBreakdown(CapacityResult result, String name) {
        return result.categoryBreakdown().stream()
                .filter(b -> b.categoryName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Category not found: " + name));
    }
}
