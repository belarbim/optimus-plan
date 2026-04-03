package com.utmost.optimusplan.infrastructure.adapter.in.web;
import com.utmost.optimusplan.domain.model.Grade;
import com.utmost.optimusplan.domain.model.GradeCostHistory;
import com.utmost.optimusplan.domain.port.in.GradeCostUseCase;
import com.utmost.optimusplan.domain.port.in.GradeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController @RequestMapping("/api/grades") @RequiredArgsConstructor
public class GradeController {
    private final GradeUseCase gradeUseCase;
    private final GradeCostUseCase gradeCostUseCase;

    record GradeRequest(@NotBlank String name, @NotNull BigDecimal dailyCost) {}
    record GradeUpdateRequest(@NotBlank String name) {}
    record GradeResponse(UUID id, String name, BigDecimal dailyCost) {
        static GradeResponse from(Grade g) { return new GradeResponse(g.getId(), g.getName(), g.getDailyCost()); }
    }

    record AddCostHistoryRequest(@NotNull BigDecimal dailyCost, @NotNull LocalDate effectiveFrom) {}
    record CostHistoryResponse(UUID id, BigDecimal dailyCost, LocalDate effectiveFrom) {
        static CostHistoryResponse from(GradeCostHistory h) {
            return new CostHistoryResponse(h.getId(), h.getDailyCost(), h.getEffectiveFrom());
        }
    }

    @GetMapping
    public List<GradeResponse> findAll() { return gradeUseCase.findAll().stream().map(GradeResponse::from).toList(); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse create(@Valid @RequestBody GradeRequest req) {
        return GradeResponse.from(gradeUseCase.create(new GradeUseCase.CreateGradeCommand(req.name(), req.dailyCost())));
    }

    @PutMapping("/{id}")
    public GradeResponse update(@PathVariable UUID id, @Valid @RequestBody GradeUpdateRequest req) {
        return GradeResponse.from(gradeUseCase.update(new GradeUseCase.UpdateGradeCommand(id, req.name())));
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { gradeUseCase.delete(id); }

    @GetMapping("/{id}/cost-history")
    public List<CostHistoryResponse> getCostHistory(@PathVariable UUID id) {
        return gradeCostUseCase.getCostHistory(id).stream().map(CostHistoryResponse::from).toList();
    }

    @PostMapping("/{id}/cost-history") @ResponseStatus(HttpStatus.CREATED)
    public CostHistoryResponse addCostHistory(@PathVariable UUID id, @Valid @RequestBody AddCostHistoryRequest req) {
        return CostHistoryResponse.from(gradeCostUseCase.addCostHistory(
                new GradeCostUseCase.AddCostHistoryCommand(id, req.dailyCost(), req.effectiveFrom())));
    }

    @GetMapping("/{id}/cost-history/current")
    public Optional<CostHistoryResponse> getCurrentCost(@PathVariable UUID id) {
        return gradeCostUseCase.getCurrentCost(id).map(CostHistoryResponse::from);
    }

    @PutMapping("/{gradeId}/cost-history/{entryId}")
    public CostHistoryResponse updateCostHistory(@PathVariable UUID gradeId, @PathVariable UUID entryId,
            @Valid @RequestBody AddCostHistoryRequest req) {
        return CostHistoryResponse.from(gradeCostUseCase.updateCostHistory(
                new GradeCostUseCase.UpdateCostHistoryCommand(entryId, req.dailyCost(), req.effectiveFrom())));
    }

    @DeleteMapping("/{gradeId}/cost-history/{entryId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCostHistory(@PathVariable UUID gradeId, @PathVariable UUID entryId) {
        gradeCostUseCase.deleteCostHistory(entryId);
    }
}
