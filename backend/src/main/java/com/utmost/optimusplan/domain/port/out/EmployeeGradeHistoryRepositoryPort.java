package com.utmost.optimusplan.domain.port.out;
import com.utmost.optimusplan.domain.model.EmployeeGradeHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface EmployeeGradeHistoryRepositoryPort {
    EmployeeGradeHistory save(EmployeeGradeHistory entry);
    List<EmployeeGradeHistory> findByEmployeeId(UUID employeeId);
    Optional<EmployeeGradeHistory> findCurrentOnDate(UUID employeeId, LocalDate date);
    Optional<EmployeeGradeHistory> findById(UUID id);
    void deleteById(UUID id);
}
