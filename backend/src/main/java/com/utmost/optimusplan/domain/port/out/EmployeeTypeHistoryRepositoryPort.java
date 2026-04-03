package com.utmost.optimusplan.domain.port.out;
import com.utmost.optimusplan.domain.model.EmployeeTypeHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeTypeHistoryRepositoryPort {
    EmployeeTypeHistory save(EmployeeTypeHistory entry);
    List<EmployeeTypeHistory> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);
    Optional<EmployeeTypeHistory> findCurrentOnDate(UUID employeeId, LocalDate date);
    Optional<EmployeeTypeHistory> findById(UUID id);
    void deleteById(UUID id);
}
