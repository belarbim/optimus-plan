package com.utmost.optimusplan.domain.port.out;
import com.utmost.optimusplan.domain.model.EmployeeCostHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface EmployeeCostHistoryRepositoryPort {
    EmployeeCostHistory save(EmployeeCostHistory entry);
    List<EmployeeCostHistory> findByEmployeeId(UUID employeeId);
    Optional<EmployeeCostHistory> findCurrentOnDate(UUID employeeId, LocalDate date);
    Optional<EmployeeCostHistory> findById(UUID id);
    void deleteById(UUID id);
}
