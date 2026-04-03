package com.utmost.optimusplan.domain.port.in;
import com.utmost.optimusplan.domain.model.EmployeeCostHistory;
import com.utmost.optimusplan.domain.model.EmployeeGradeHistory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface EmployeeCostUseCase {
    record AddGradeHistoryCommand(UUID employeeId, UUID gradeId, LocalDate effectiveFrom) {}
    record UpdateGradeHistoryCommand(UUID id, UUID gradeId, LocalDate effectiveFrom) {}
    record AddCostHistoryCommand(UUID employeeId, BigDecimal dailyCost, LocalDate effectiveFrom) {}
    record UpdateCostHistoryCommand(UUID id, BigDecimal dailyCost, LocalDate effectiveFrom) {}

    EmployeeGradeHistory addGradeHistory(AddGradeHistoryCommand cmd);
    List<EmployeeGradeHistory> getGradeHistory(UUID employeeId);
    Optional<EmployeeGradeHistory> getCurrentGrade(UUID employeeId);
    EmployeeGradeHistory updateGradeHistory(UpdateGradeHistoryCommand cmd);
    void deleteGradeHistory(UUID id);

    EmployeeCostHistory addCostHistory(AddCostHistoryCommand cmd);
    List<EmployeeCostHistory> getCostHistory(UUID employeeId);
    Optional<EmployeeCostHistory> getCurrentCost(UUID employeeId);
    EmployeeCostHistory updateCostHistory(UpdateCostHistoryCommand cmd);
    void deleteCostHistory(UUID id);
}
