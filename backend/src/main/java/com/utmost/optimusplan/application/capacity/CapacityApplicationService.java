package com.utmost.optimusplan.application.capacity;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.CategoryAllocation;
import com.utmost.optimusplan.domain.model.PublicHoliday;
import com.utmost.optimusplan.domain.model.RoleHistory;
import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.model.TeamAssignment;
import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import com.utmost.optimusplan.domain.port.in.CapacityUseCase;
import com.utmost.optimusplan.domain.port.out.AssignmentRepositoryPort;
import com.utmost.optimusplan.domain.port.out.CategoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.HolidayRepositoryPort;
import com.utmost.optimusplan.domain.port.out.RoleHistoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.SnapshotRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import com.utmost.optimusplan.domain.port.out.WorkingDaysRepositoryPort;
import com.utmost.optimusplan.domain.service.BusinessDayCalculator;
import com.utmost.optimusplan.domain.service.CapacityCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CapacityApplicationService implements CapacityUseCase {

    private static final String DEFAULT_LOCALE = "FR";

    private final TeamRepositoryPort        teamRepo;
    private final AssignmentRepositoryPort  assignmentRepo;
    private final RoleHistoryRepositoryPort roleHistoryRepo;
    private final CategoryRepositoryPort    categoryRepo;
    private final HolidayRepositoryPort     holidayRepo;
    private final WorkingDaysRepositoryPort workingDaysRepo;
    private final SnapshotRepositoryPort    snapshotRepo;

    public CapacityApplicationService(TeamRepositoryPort teamRepo,
                                       AssignmentRepositoryPort assignmentRepo,
                                       RoleHistoryRepositoryPort roleHistoryRepo,
                                       CategoryRepositoryPort categoryRepo,
                                       HolidayRepositoryPort holidayRepo,
                                       WorkingDaysRepositoryPort workingDaysRepo,
                                       SnapshotRepositoryPort snapshotRepo) {
        this.teamRepo        = teamRepo;
        this.assignmentRepo  = assignmentRepo;
        this.roleHistoryRepo = roleHistoryRepo;
        this.categoryRepo    = categoryRepo;
        this.holidayRepo     = holidayRepo;
        this.workingDaysRepo = workingDaysRepo;
        this.snapshotRepo    = snapshotRepo;
    }

    // -------------------------------------------------------------------------
    // CapacityUseCase
    // -------------------------------------------------------------------------

    @Override
    public CapacityResult computeCapacity(ComputeCapacityQuery query) {
        Team team = requireTeam(query.teamId());
        List<Team> children = teamRepo.findByParentId(query.teamId());
        if (children.isEmpty()) {
            return doCompute(team, query.month());
        }
        return aggregateWithChildren(team, query.month(), children);
    }

    @Override
    public RemainingCapacityResult computeRemaining(RemainingCapacityQuery query) {
        Team team = requireTeam(query.teamId());

        LocalDate date      = query.date();
        YearMonth ym        = YearMonth.from(date);
        String    month     = ym.toString();
        LocalDate monthEnd  = ym.atEndOfMonth();

        // Full-month capacity for the ratio
        CapacityResult fullResult = doCompute(team, month);

        List<PublicHoliday> holidays = loadHolidays(month);
        int[] remainingTotal = BusinessDayCalculator.remainingAndTotal(date, monthEnd, holidays);
        int remaining = remainingTotal[0];
        int total     = remainingTotal[1];

        // Determine the adjusted reference date (first weekday on or after the requested date)
        LocalDate adjusted = date;
        while (!adjusted.isAfter(monthEnd) && !BusinessDayCalculator.isWeekday(adjusted)) {
            adjusted = adjusted.plusDays(1);
        }

        BigDecimal ratio = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(remaining).divide(BigDecimal.valueOf(total), 10, RoundingMode.HALF_UP);

        BigDecimal totalRemaining = fullResult.totalCapacity().multiply(ratio)
                .setScale(3, RoundingMode.HALF_UP);

        List<CategoryBreakdown> remainingBreakdown = fullResult.categoryBreakdown().stream()
                .map(cb -> new CategoryBreakdown(
                        cb.categoryName(),
                        cb.manDays().multiply(ratio).setScale(3, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());

        return new RemainingCapacityResult(date, adjusted, remaining, total,
                totalRemaining, remainingBreakdown);
    }

    @Override
    public RollupResult computeRollup(RollupQuery query) {
        Team team = requireTeam(query.teamId());
        CapacityResult ownCapacity = doCompute(team, query.month());

        List<Team> subTeams = teamRepo.findByParentId(query.teamId());
        List<CapacityResult> subResults = subTeams.stream()
                .map(sub -> doCompute(sub, query.month()))
                .collect(Collectors.toList());

        BigDecimal consolidated = subResults.stream()
                .map(CapacityResult::totalCapacity)
                .reduce(ownCapacity.totalCapacity(), BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        return new RollupResult(ownCapacity, subResults, consolidated);
    }

    @Override
    public SimulationResult simulate(SimulationQuery query) {
        Team team = requireTeam(query.teamId());

        // Baseline
        CapacityResult baseline = doCompute(team, query.month());

        // Build a mutable copy of assignments and apply the simulated changes
        YearMonth ym    = YearMonth.parse(query.month());
        LocalDate from  = ym.atDay(1);
        LocalDate to    = ym.atEndOfMonth();

        List<TeamAssignment> assignments = new ArrayList<>(
                assignmentRepo.findActiveByTeamIdAndMonth(query.teamId(), from, to));

        List<String> warnings = new ArrayList<>();

        for (SimulatedChange change : query.changes()) {
            switch (change.type().toUpperCase()) {
                case "ADD" -> {
                    TeamAssignment fake = TeamAssignment.builder()
                            .id(UUID.randomUUID())
                            .teamId(query.teamId())
                            .employeeId(change.employeeId())
                            .employeeName("Simulated")
                            .allocationPct(change.allocationPct())
                            .roleType(change.roleType())
                            .roleWeight(change.roleWeight())
                            .startDate(from)
                            .endDate(null)
                            .build();
                    assignments.add(fake);
                    if (change.allocationPct().compareTo(BigDecimal.valueOf(90)) >= 0) {
                        warnings.add("Simulated addition of employee %s at %.0f%% is near capacity limit"
                                .formatted(change.employeeId(), change.allocationPct()));
                    }
                }
                case "REMOVE" -> assignments.removeIf(a ->
                        a.getEmployeeId().equals(change.employeeId()));
                case "MODIFY" -> assignments.stream()
                        .filter(a -> a.getEmployeeId().equals(change.employeeId()))
                        .forEach(a -> {
                            if (change.allocationPct() != null) a.setAllocationPct(change.allocationPct());
                            if (change.roleType()      != null) a.setRoleType(change.roleType());
                            if (change.roleWeight()    != null) a.setRoleWeight(change.roleWeight());
                        });
                default -> warnings.add("Unknown simulation change type: " + change.type());
            }
        }

        // Role histories: load existing ones; simulated additions have none
        List<UUID> assignmentIds = assignments.stream().map(TeamAssignment::getId).toList();
        List<RoleHistory> histories = assignmentIds.isEmpty() ? List.of()
                : assignmentIds.stream()
                .flatMap(id -> roleHistoryRepo.findByAssignmentId(id).stream())
                .collect(Collectors.toList());

        List<CategoryAllocation> categories = categoryRepo.findByTeamId(query.teamId());
        List<PublicHoliday> holidays        = loadHolidays(query.month());
        Optional<WorkingDaysConfig> wdc     = workingDaysRepo.findByMonth(query.month());

        CapacityResult simulated = CapacityCalculator.compute(
                team, query.month(), assignments, categories, histories, holidays, wdc);

        // Build per-category deltas
        Map<String, BigDecimal> deltas = new LinkedHashMap<>();
        Map<String, BigDecimal> baseMap = baseline.categoryBreakdown().stream()
                .collect(Collectors.toMap(CategoryBreakdown::categoryName, CategoryBreakdown::manDays));
        for (CategoryBreakdown cb : simulated.categoryBreakdown()) {
            BigDecimal base = baseMap.getOrDefault(cb.categoryName(), BigDecimal.ZERO);
            deltas.put(cb.categoryName(), cb.manDays().subtract(base).setScale(3, RoundingMode.HALF_UP));
        }
        deltas.put("total", simulated.totalCapacity()
                .subtract(baseline.totalCapacity()).setScale(3, RoundingMode.HALF_UP));

        return new SimulationResult(baseline, simulated, deltas, warnings);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private CapacityResult aggregateWithChildren(Team team, String month, List<Team> children) {
        List<CapacityResult> childResults = children.stream()
                .map(child -> {
                    List<Team> grandChildren = teamRepo.findByParentId(child.getId());
                    return grandChildren.isEmpty()
                            ? doCompute(child, month)
                            : aggregateWithChildren(child, month, grandChildren);
                })
                .collect(Collectors.toList());

        CapacityResult ownResult = doCompute(team, month);

        BigDecimal total = childResults.stream()
                .map(CapacityResult::totalCapacity)
                .reduce(ownResult.totalCapacity(), BigDecimal::add)
                .setScale(3, RoundingMode.HALF_UP);

        Map<String, BigDecimal> catMap = new LinkedHashMap<>();
        ownResult.categoryBreakdown().forEach(cb -> catMap.merge(cb.categoryName(), cb.manDays(), BigDecimal::add));
        childResults.forEach(cr -> cr.categoryBreakdown().forEach(cb ->
                catMap.merge(cb.categoryName(), cb.manDays(), BigDecimal::add)));
        List<CategoryBreakdown> mergedBreakdown = catMap.entrySet().stream()
                .map(e -> new CategoryBreakdown(e.getKey(), e.getValue().setScale(3, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());

        List<EmployeeContribution> mergedContributions = new ArrayList<>(ownResult.employeeContributions());
        childResults.forEach(cr -> mergedContributions.addAll(cr.employeeContributions()));

        return new CapacityResult(team.getId(), team.getName(), month, total, mergedBreakdown, mergedContributions);
    }

    private Team requireTeam(UUID teamId) {
        return teamRepo.findById(teamId)
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("Team", teamId)));
    }

    private CapacityResult doCompute(Team team, String month) {
        YearMonth ym   = YearMonth.parse(month);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        List<TeamAssignment>   assignments  = assignmentRepo.findActiveByTeamIdAndMonth(team.getId(), from, to);
        List<CategoryAllocation> categories = categoryRepo.findByTeamId(team.getId());
        List<PublicHoliday>    holidays     = loadHolidays(month);
        Optional<WorkingDaysConfig> wdc     = workingDaysRepo.findByMonth(month);

        // Gather all role histories for the assignments in one pass
        List<RoleHistory> histories = assignments.isEmpty() ? List.of()
                : assignments.stream()
                .flatMap(a -> roleHistoryRepo.findByAssignmentId(a.getId()).stream())
                .collect(Collectors.toList());

        return CapacityCalculator.compute(team, month, assignments, categories, histories, holidays, wdc);
    }

    private List<PublicHoliday> loadHolidays(String month) {
        return holidayRepo.findByMonthAndLocale(month, DEFAULT_LOCALE);
    }
}
