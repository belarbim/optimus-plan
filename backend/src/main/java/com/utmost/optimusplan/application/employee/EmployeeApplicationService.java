package com.utmost.optimusplan.application.employee;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.Employee;
import com.utmost.optimusplan.domain.port.in.EmployeeUseCase;
import com.utmost.optimusplan.domain.port.out.AssignmentRepositoryPort;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.EmployeeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class EmployeeApplicationService implements EmployeeUseCase {

    private final EmployeeRepositoryPort   employeeRepo;
    private final AssignmentRepositoryPort assignmentRepo;
    private final AuditRepositoryPort      auditRepo;

    public EmployeeApplicationService(EmployeeRepositoryPort employeeRepo,
                                       AssignmentRepositoryPort assignmentRepo,
                                       AuditRepositoryPort auditRepo) {
        this.employeeRepo   = employeeRepo;
        this.assignmentRepo = assignmentRepo;
        this.auditRepo      = auditRepo;
    }

    // -------------------------------------------------------------------------
    // EmployeeUseCase
    // -------------------------------------------------------------------------

    @Override
    public Employee create(CreateEmployeeCommand cmd) {
        if (employeeRepo.existsByEmail(cmd.email())) {
            throw new DomainException(new DomainError.Conflict(
                    "An employee with email '" + cmd.email() + "' already exists"));
        }

        LocalDateTime now = LocalDateTime.now();
        Employee employee = Employee.builder()
                .id(UUID.randomUUID())
                .firstName(cmd.firstName())
                .lastName(cmd.lastName())
                .email(cmd.email())
                .type(cmd.type() != null ? cmd.type() : "INTERNAL")
                .createdAt(now)
                .updatedAt(now)
                .build();

        Employee saved = employeeRepo.save(employee);
        audit("Employee", saved.getId(), "CREATE",
                Map.of("email", saved.getEmail(), "name", saved.fullName()));
        return saved;
    }

    @Override
    public Employee update(UpdateEmployeeCommand cmd) {
        Employee employee = findById(cmd.id());

        // If the e-mail address is being changed, ensure the new one is unique
        if (!employee.getEmail().equalsIgnoreCase(cmd.email())
                && employeeRepo.existsByEmail(cmd.email())) {
            throw new DomainException(new DomainError.Conflict(
                    "An employee with email '" + cmd.email() + "' already exists"));
        }

        employee.setFirstName(cmd.firstName());
        employee.setLastName(cmd.lastName());
        employee.setEmail(cmd.email());
        employee.setType(cmd.type() != null ? cmd.type() : employee.getType());
        employee.setUpdatedAt(LocalDateTime.now());

        Employee saved = employeeRepo.save(employee);
        audit("Employee", saved.getId(), "UPDATE",
                Map.of("email", saved.getEmail(), "name", saved.fullName()));
        return saved;
    }

    @Override
    public void delete(UUID id) {
        Employee employee = findById(id);

        if (assignmentRepo.hasActiveAssignmentsByEmployeeId(id)) {
            throw new DomainException(new DomainError.BusinessRule(
                    "Employee has active assignments"));
        }

        employeeRepo.deleteById(id);
        audit("Employee", id, "DELETE",
                Map.of("email", employee.getEmail(), "name", employee.fullName()));
    }

    @Override
    @Transactional(readOnly = true)
    public Employee findById(UUID id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new DomainException(
                        new DomainError.NotFound("Employee", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepo.findAll();
    }

    @Override
    public EmployeeUseCase.ImportResult importBatch(List<CreateEmployeeCommand> commands) {
        int imported = 0;
        int skipped  = 0;
        List<EmployeeUseCase.ImportRowError> errors = new ArrayList<>();

        for (int i = 0; i < commands.size(); i++) {
            CreateEmployeeCommand cmd = commands.get(i);
            int row = i + 2; // +2 because row 1 is the header
            try {
                create(cmd);
                imported++;
            } catch (DomainException ex) {
                if (ex.error() instanceof DomainError.Conflict) {
                    skipped++;
                } else {
                    errors.add(new EmployeeUseCase.ImportRowError(row, cmd.email(), ex.getMessage()));
                }
            } catch (Exception ex) {
                errors.add(new EmployeeUseCase.ImportRowError(row, cmd.email(), ex.getMessage()));
            }
        }
        return new EmployeeUseCase.ImportResult(imported, skipped, errors);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void audit(String entityType, UUID id, String action, Map<String, Object> changes) {
        auditRepo.save(AuditLog.builder()
                .id(UUID.randomUUID())
                .entityType(entityType)
                .entityId(id)
                .action(action)
                .changes(changes)
                .actor("manager")
                .timestamp(LocalDateTime.now())
                .build());
    }
}
