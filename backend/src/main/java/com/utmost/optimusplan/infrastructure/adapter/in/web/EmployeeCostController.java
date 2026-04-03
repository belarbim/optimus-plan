package com.utmost.optimusplan.infrastructure.adapter.in.web;
import com.utmost.optimusplan.domain.model.EmployeeCostHistory;
import com.utmost.optimusplan.domain.model.EmployeeGradeHistory;
import com.utmost.optimusplan.domain.model.EmployeeTypeHistory;
import com.utmost.optimusplan.domain.port.in.EmployeeCostUseCase;
import com.utmost.optimusplan.domain.port.in.EmployeeTypeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController @RequestMapping("/api/employees") @RequiredArgsConstructor
public class EmployeeCostController {
    private final EmployeeCostUseCase useCase;
    private final EmployeeTypeUseCase employeeTypeUseCase;

    record AddGradeHistoryRequest(@NotNull UUID gradeId, @NotNull LocalDate effectiveFrom) {}
    record GradeHistoryResponse(UUID id, UUID gradeId, String gradeName, BigDecimal dailyCost, LocalDate effectiveFrom) {
        static GradeHistoryResponse from(EmployeeGradeHistory h) {
            return new GradeHistoryResponse(h.getId(), h.getGradeId(), h.getGradeName(), h.getDailyCost(), h.getEffectiveFrom());
        }
    }

    record AddCostHistoryRequest(@NotNull BigDecimal dailyCost, @NotNull LocalDate effectiveFrom) {}
    record CostHistoryResponse(UUID id, BigDecimal dailyCost, LocalDate effectiveFrom) {
        static CostHistoryResponse from(EmployeeCostHistory h) {
            return new CostHistoryResponse(h.getId(), h.getDailyCost(), h.getEffectiveFrom());
        }
    }

    record AddTypeHistoryRequest(@NotNull String type, @NotNull LocalDate effectiveFrom) {}
    record TypeHistoryResponse(UUID id, String type, LocalDate effectiveFrom) {
        static TypeHistoryResponse from(EmployeeTypeHistory h) {
            return new TypeHistoryResponse(h.getId(), h.getType(), h.getEffectiveFrom());
        }
    }

    @GetMapping("/{id}/grade-history")
    public List<GradeHistoryResponse> getGradeHistory(@PathVariable UUID id) {
        return useCase.getGradeHistory(id).stream().map(GradeHistoryResponse::from).toList();
    }

    @PostMapping("/{id}/grade-history") @ResponseStatus(HttpStatus.CREATED)
    public GradeHistoryResponse addGradeHistory(@PathVariable UUID id, @Valid @RequestBody AddGradeHistoryRequest req) {
        return GradeHistoryResponse.from(useCase.addGradeHistory(new EmployeeCostUseCase.AddGradeHistoryCommand(id, req.gradeId(), req.effectiveFrom())));
    }

    @GetMapping("/{id}/grade-history/current")
    public Optional<GradeHistoryResponse> getCurrentGrade(@PathVariable UUID id) {
        return useCase.getCurrentGrade(id).map(GradeHistoryResponse::from);
    }

    @GetMapping("/{id}/cost-history")
    public List<CostHistoryResponse> getCostHistory(@PathVariable UUID id) {
        return useCase.getCostHistory(id).stream().map(CostHistoryResponse::from).toList();
    }

    @PostMapping("/{id}/cost-history") @ResponseStatus(HttpStatus.CREATED)
    public CostHistoryResponse addCostHistory(@PathVariable UUID id, @Valid @RequestBody AddCostHistoryRequest req) {
        return CostHistoryResponse.from(useCase.addCostHistory(new EmployeeCostUseCase.AddCostHistoryCommand(id, req.dailyCost(), req.effectiveFrom())));
    }

    @GetMapping("/{id}/cost-history/current")
    public Optional<CostHistoryResponse> getCurrentCost(@PathVariable UUID id) {
        return useCase.getCurrentCost(id).map(CostHistoryResponse::from);
    }

    @GetMapping("/{id}/type-history")
    public List<TypeHistoryResponse> getTypeHistory(@PathVariable UUID id) {
        return employeeTypeUseCase.getTypeHistory(id).stream().map(TypeHistoryResponse::from).toList();
    }

    @PostMapping("/{id}/type-history") @ResponseStatus(HttpStatus.CREATED)
    public TypeHistoryResponse addTypeHistory(@PathVariable UUID id, @Valid @RequestBody AddTypeHistoryRequest req) {
        return TypeHistoryResponse.from(employeeTypeUseCase.addTypeHistory(
                new EmployeeTypeUseCase.AddTypeHistoryCommand(id, req.type(), req.effectiveFrom())));
    }

    @GetMapping("/{id}/type-history/current")
    public Optional<TypeHistoryResponse> getCurrentType(@PathVariable UUID id) {
        return employeeTypeUseCase.getCurrentType(id).map(TypeHistoryResponse::from);
    }

    @PutMapping("/{employeeId}/type-history/{entryId}")
    public TypeHistoryResponse updateTypeHistory(@PathVariable UUID employeeId, @PathVariable UUID entryId,
            @Valid @RequestBody AddTypeHistoryRequest req) {
        return TypeHistoryResponse.from(employeeTypeUseCase.updateTypeHistory(
                new EmployeeTypeUseCase.UpdateTypeHistoryCommand(entryId, req.type(), req.effectiveFrom())));
    }

    @DeleteMapping("/{employeeId}/type-history/{entryId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTypeHistory(@PathVariable UUID employeeId, @PathVariable UUID entryId) {
        employeeTypeUseCase.deleteTypeHistory(entryId);
    }

    @PutMapping("/{employeeId}/grade-history/{entryId}")
    public GradeHistoryResponse updateGradeHistory(@PathVariable UUID employeeId, @PathVariable UUID entryId,
            @Valid @RequestBody AddGradeHistoryRequest req) {
        return GradeHistoryResponse.from(useCase.updateGradeHistory(
                new EmployeeCostUseCase.UpdateGradeHistoryCommand(entryId, req.gradeId(), req.effectiveFrom())));
    }

    @DeleteMapping("/{employeeId}/grade-history/{entryId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGradeHistory(@PathVariable UUID employeeId, @PathVariable UUID entryId) {
        useCase.deleteGradeHistory(entryId);
    }

    @PutMapping("/{employeeId}/cost-history/{entryId}")
    public CostHistoryResponse updateCostHistory(@PathVariable UUID employeeId, @PathVariable UUID entryId,
            @Valid @RequestBody AddCostHistoryRequest req) {
        return CostHistoryResponse.from(useCase.updateCostHistory(
                new EmployeeCostUseCase.UpdateCostHistoryCommand(entryId, req.dailyCost(), req.effectiveFrom())));
    }

    @DeleteMapping("/{employeeId}/cost-history/{entryId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCostHistory(@PathVariable UUID employeeId, @PathVariable UUID entryId) {
        useCase.deleteCostHistory(entryId);
    }
}
