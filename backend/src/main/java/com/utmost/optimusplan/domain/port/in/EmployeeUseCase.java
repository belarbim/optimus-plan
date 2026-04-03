package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.Employee;

import java.util.List;
import java.util.UUID;

public interface EmployeeUseCase {

    record CreateEmployeeCommand(String firstName, String lastName, String email, String type) {}

    record UpdateEmployeeCommand(UUID id, String firstName, String lastName, String email, String type) {}

    record ImportRowError(int row, String email, String reason) {}

    record ImportResult(int imported, int skipped, List<ImportRowError> errors, List<Employee> importedEmployees) {}

    Employee create(CreateEmployeeCommand cmd);

    Employee update(UpdateEmployeeCommand cmd);

    void delete(UUID id);

    Employee findById(UUID id);

    List<Employee> findAll();

    ImportResult importBatch(List<CreateEmployeeCommand> commands);
}
