package com.utmost.optimusplan.domain.port.in;
import com.utmost.optimusplan.domain.model.GradeCostHistory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeCostUseCase {
    record AddCostHistoryCommand(UUID gradeId, BigDecimal dailyCost, LocalDate effectiveFrom) {}
    record UpdateCostHistoryCommand(UUID id, BigDecimal dailyCost, LocalDate effectiveFrom) {}
    GradeCostHistory addCostHistory(AddCostHistoryCommand cmd);
    List<GradeCostHistory> getCostHistory(UUID gradeId);
    Optional<GradeCostHistory> getCurrentCost(UUID gradeId);
    GradeCostHistory updateCostHistory(UpdateCostHistoryCommand cmd);
    void deleteCostHistory(UUID id);
}
