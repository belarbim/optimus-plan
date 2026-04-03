package com.utmost.optimusplan.application.employeetype;
import com.utmost.optimusplan.domain.model.EmployeeTypeHistory;
import com.utmost.optimusplan.domain.port.in.EmployeeTypeUseCase;
import com.utmost.optimusplan.domain.port.out.EmployeeRepositoryPort;
import com.utmost.optimusplan.domain.port.out.EmployeeTypeHistoryRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service @Transactional
public class EmployeeTypeApplicationService implements EmployeeTypeUseCase {
    private final EmployeeTypeHistoryRepositoryPort typeHistoryRepo;
    private final EmployeeRepositoryPort employeeRepo;

    public EmployeeTypeApplicationService(EmployeeTypeHistoryRepositoryPort typeHistoryRepo, EmployeeRepositoryPort employeeRepo) {
        this.typeHistoryRepo = typeHistoryRepo;
        this.employeeRepo = employeeRepo;
    }

    @Override
    public EmployeeTypeHistory addTypeHistory(AddTypeHistoryCommand cmd) {
        if (!cmd.type().equals("INTERNAL") && !cmd.type().equals("EXTERNAL"))
            throw new IllegalArgumentException("Type must be INTERNAL or EXTERNAL");

        EmployeeTypeHistory entry = EmployeeTypeHistory.builder()
                .id(UUID.randomUUID())
                .employeeId(cmd.employeeId())
                .type(cmd.type())
                .effectiveFrom(cmd.effectiveFrom())
                .build();
        EmployeeTypeHistory saved = typeHistoryRepo.save(entry);

        // Update denormalized type on employee if this entry is now the current one
        Optional<EmployeeTypeHistory> current = typeHistoryRepo.findCurrentOnDate(cmd.employeeId(), LocalDate.now());
        if (current.isPresent() && current.get().getId().equals(saved.getId())) {
            employeeRepo.findById(cmd.employeeId()).ifPresent(emp -> {
                emp.setType(cmd.type());
                employeeRepo.save(emp);
            });
        }

        return saved;
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeTypeHistory> getTypeHistory(UUID employeeId) {
        return typeHistoryRepo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId);
    }

    @Override @Transactional(readOnly = true)
    public Optional<EmployeeTypeHistory> getCurrentType(UUID employeeId) {
        return typeHistoryRepo.findCurrentOnDate(employeeId, LocalDate.now());
    }

    @Override
    public EmployeeTypeHistory updateTypeHistory(UpdateTypeHistoryCommand cmd) {
        EmployeeTypeHistory existing = typeHistoryRepo.findById(cmd.id())
                .orElseThrow(() -> new RuntimeException("Type history entry not found: " + cmd.id()));
        if (!cmd.type().equals("INTERNAL") && !cmd.type().equals("EXTERNAL"))
            throw new IllegalArgumentException("Type must be INTERNAL or EXTERNAL");
        existing.setType(cmd.type());
        existing.setEffectiveFrom(cmd.effectiveFrom());
        EmployeeTypeHistory saved = typeHistoryRepo.save(existing);

        // Update denormalized type on employee if this entry is now the current one
        Optional<EmployeeTypeHistory> current = typeHistoryRepo.findCurrentOnDate(existing.getEmployeeId(), LocalDate.now());
        if (current.isPresent() && current.get().getId().equals(saved.getId())) {
            employeeRepo.findById(existing.getEmployeeId()).ifPresent(emp -> {
                emp.setType(cmd.type());
                employeeRepo.save(emp);
            });
        }

        return saved;
    }

    @Override
    public void deleteTypeHistory(UUID id) {
        typeHistoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Type history entry not found: " + id));
        typeHistoryRepo.deleteById(id);
    }
}
