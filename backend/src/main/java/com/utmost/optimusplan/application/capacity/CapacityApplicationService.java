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
import com.utmost.optimusplan.domain.port.out.TeamTypeRepositoryPort;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CapacityApplicationService implements CapacityUseCase {


    private final TeamRepositoryPort        teamRepo;
    private final TeamTypeRepositoryPort    teamTypeRepo;
    private final AssignmentRepositoryPort  assignmentRepo;
    private final RoleHistoryRepositoryPort roleHistoryRepo;
    private final CategoryRepositoryPort    categoryRepo;
    private final HolidayRepositoryPort     holidayRepo;
    private final WorkingDaysRepositoryPort workingDaysRepo;
    private final SnapshotRepositoryPort    snapshotRepo;

    public CapacityApplicationService(TeamRepositoryPort teamRepo,
                                       TeamTypeRepositoryPort teamTypeRepo,
                                       AssignmentRepositoryPort assignmentRepo,
                                       RoleHistoryRepositoryPort roleHistoryRepo,
                                       CategoryRepositoryPort categoryRepo,
                                       HolidayRepositoryPort holidayRepo,
                                       WorkingDaysRepositoryPort workingDaysRepo,
                                       SnapshotRepositoryPort snapshotRepo) {
        this.teamRepo        = teamRepo;
        this.teamTypeRepo    = teamTypeRepo;
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

        LocalDate date     = query.date();
        int       year     = date.getYear();
        YearMonth firstYm  = YearMonth.from(date);
        YearMonth yearEnd  = YearMonth.of(year, 12);

        // Adjust reference date to the next weekday when it falls on a weekend
        LocalDate monthEnd = firstYm.atEndOfMonth();
        LocalDate adjusted = date;
        while (!adjusted.isAfter(monthEnd) && !BusinessDayCalculator.isWeekday(adjusted)) {
            adjusted = adjusted.plusDays(1);
        }

        // --- Current month (partial): apply remaining/total ratio ---
        List<PublicHoliday> firstHolidays = loadHolidays(firstYm.toString());
        int[] rt              = BusinessDayCalculator.remainingAndTotal(date, monthEnd, firstHolidays);
        int   remainingFirst  = rt[0];
        int   totalFirst      = rt[1];

        BigDecimal ratio = totalFirst == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(remainingFirst)
                        .divide(BigDecimal.valueOf(totalFirst), 10, RoundingMode.HALF_UP);

        CapacityResult firstResult = doCompute(team, firstYm.toString());
        BigDecimal totalRemaining  = firstResult.totalCapacity().multiply(ratio).setScale(3, RoundingMode.HALF_UP);

        Map<String, BigDecimal> catMap = new LinkedHashMap<>();
        firstResult.categoryBreakdown().forEach(cb ->
                catMap.put(cb.categoryName(), cb.manDays().multiply(ratio).setScale(3, RoundingMode.HALF_UP)));

        int remainingBusinessDays = remainingFirst;

        // --- Subsequent full months until December ---
        YearMonth ym = firstYm.plusMonths(1);
        while (!ym.isAfter(yearEnd)) {
            String              monthStr  = ym.toString();
            List<PublicHoliday> holidays  = loadHolidays(monthStr);
            CapacityResult      monthResult = doCompute(team, monthStr);
            int                 bizDays   = BusinessDayCalculator.countBusinessDays(
                                                ym.atDay(1), ym.atEndOfMonth(), holidays);

            remainingBusinessDays += bizDays;
            totalRemaining = totalRemaining.add(monthResult.totalCapacity()).setScale(3, RoundingMode.HALF_UP);
            monthResult.categoryBreakdown().forEach(cb ->
                    catMap.merge(cb.categoryName(), cb.manDays(), BigDecimal::add));
            ym = ym.plusMonths(1);
        }

        // --- Total business days for the full year (for context) ---
        int totalYearDays = 0;
        for (int m = 1; m <= 12; m++) {
            YearMonth yym = YearMonth.of(year, m);
            totalYearDays += BusinessDayCalculator.countBusinessDays(
                    yym.atDay(1), yym.atEndOfMonth(), loadHolidays(yym.toString()));
        }

        List<CategoryBreakdown> remainingBreakdown = catMap.entrySet().stream()
                .map(e -> new CategoryBreakdown(e.getKey(), e.getValue().setScale(3, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());

        return new RemainingCapacityResult(date, adjusted, remainingBusinessDays, totalYearDays,
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
        Set<String> totalCatNames           = resolveTotalCapacityCategoryNames(team);

        CapacityResult simulated = CapacityCalculator.compute(
                team, query.month(), assignments, categories, histories, holidays, wdc, totalCatNames);

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

        Set<String> totalCatNames = resolveTotalCapacityCategoryNames(team);

        return CapacityCalculator.compute(team, month, assignments, categories, histories, holidays, wdc, totalCatNames);
    }

    /**
     * Returns the lower-cased names of categories flagged as isPartOfTotalCapacity
     * for the given team's team type. Returns an empty set when the team has no type.
     */
    private Set<String> resolveTotalCapacityCategoryNames(Team team) {
        if (team.getTeamTypeId() == null) {
            return Set.of();
        }
        return teamTypeRepo.findById(team.getTeamTypeId())
                .map(tt -> tt.getCategories().stream()
                        .filter(com.utmost.optimusplan.domain.model.TeamTypeCategory::isPartOfTotalCapacity)
                        .map(c -> c.getName().toLowerCase())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private List<PublicHoliday> loadHolidays(String month) {
        return holidayRepo.findByMonth(month);
    }
}
