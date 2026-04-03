package com.utmost.optimusplan.domain.port.in;
import com.utmost.optimusplan.domain.model.EmployeeTypeHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeTypeUseCase {
    record AddTypeHistoryCommand(UUID employeeId, String type, LocalDate effectiveFrom) {}
    record UpdateTypeHistoryCommand(UUID id, String type, LocalDate effectiveFrom) {}
    EmployeeTypeHistory addTypeHistory(AddTypeHistoryCommand cmd);
    List<EmployeeTypeHistory> getTypeHistory(UUID employeeId);
    Optional<EmployeeTypeHistory> getCurrentType(UUID employeeId);
    EmployeeTypeHistory updateTypeHistory(UpdateTypeHistoryCommand cmd);
    void deleteTypeHistory(UUID id);
}
