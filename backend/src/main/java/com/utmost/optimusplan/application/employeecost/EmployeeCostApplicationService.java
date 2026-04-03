package com.utmost.optimusplan.application.employeecost;
import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.EmployeeCostHistory;
import com.utmost.optimusplan.domain.model.EmployeeGradeHistory;
import com.utmost.optimusplan.domain.port.in.EmployeeCostUseCase;
import com.utmost.optimusplan.domain.port.out.EmployeeCostHistoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.EmployeeGradeHistoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.EmployeeRepositoryPort;
import com.utmost.optimusplan.domain.port.out.GradeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service @Transactional
public class EmployeeCostApplicationService implements EmployeeCostUseCase {
    private final EmployeeRepositoryPort employeeRepo;
    private final GradeRepositoryPort gradeRepo;
    private final EmployeeGradeHistoryRepositoryPort gradeHistoryRepo;
    private final EmployeeCostHistoryRepositoryPort costHistoryRepo;

    public EmployeeCostApplicationService(EmployeeRepositoryPort employeeRepo,
            GradeRepositoryPort gradeRepo,
            EmployeeGradeHistoryRepositoryPort gradeHistoryRepo,
            EmployeeCostHistoryRepositoryPort costHistoryRepo) {
        this.employeeRepo = employeeRepo;
        this.gradeRepo = gradeRepo;
        this.gradeHistoryRepo = gradeHistoryRepo;
        this.costHistoryRepo = costHistoryRepo;
    }

    @Override
    public EmployeeGradeHistory addGradeHistory(AddGradeHistoryCommand cmd) {
        employeeRepo.findById(cmd.employeeId())
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("Employee", cmd.employeeId())));
        gradeRepo.findById(cmd.gradeId())
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("Grade", cmd.gradeId())));
        return gradeHistoryRepo.save(EmployeeGradeHistory.builder()
                .id(UUID.randomUUID()).employeeId(cmd.employeeId()).gradeId(cmd.gradeId())
                .effectiveFrom(cmd.effectiveFrom()).createdAt(LocalDateTime.now()).build());
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeGradeHistory> getGradeHistory(UUID employeeId) {
        return gradeHistoryRepo.findByEmployeeId(employeeId);
    }

    @Override @Transactional(readOnly = true)
    public Optional<EmployeeGradeHistory> getCurrentGrade(UUID employeeId) {
        return gradeHistoryRepo.findCurrentOnDate(employeeId, LocalDate.now());
    }

    @Override
    public EmployeeCostHistory addCostHistory(AddCostHistoryCommand cmd) {
        employeeRepo.findById(cmd.employeeId())
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("Employee", cmd.employeeId())));
        if (cmd.dailyCost() == null || cmd.dailyCost().signum() < 0)
            throw new DomainException(new DomainError.BusinessRule("Daily cost must be >= 0"));
        return costHistoryRepo.save(EmployeeCostHistory.builder()
                .id(UUID.randomUUID()).employeeId(cmd.employeeId()).dailyCost(cmd.dailyCost())
                .effectiveFrom(cmd.effectiveFrom()).createdAt(LocalDateTime.now()).build());
    }

    @Override @Transactional(readOnly = true)
    public List<EmployeeCostHistory> getCostHistory(UUID employeeId) {
        return costHistoryRepo.findByEmployeeId(employeeId);
    }

    @Override @Transactional(readOnly = true)
    public Optional<EmployeeCostHistory> getCurrentCost(UUID employeeId) {
        return costHistoryRepo.findCurrentOnDate(employeeId, LocalDate.now());
    }

    @Override
    public EmployeeGradeHistory updateGradeHistory(UpdateGradeHistoryCommand cmd) {
        EmployeeGradeHistory existing = gradeHistoryRepo.findById(cmd.id())
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("GradeHistory", cmd.id())));
        gradeRepo.findById(cmd.gradeId())
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("Grade", cmd.gradeId())));
        existing.setGradeId(cmd.gradeId());
        existing.setEffectiveFrom(cmd.effectiveFrom());
        return gradeHistoryRepo.save(existing);
    }

    @Override
    public void deleteGradeHistory(UUID id) {
        gradeHistoryRepo.findById(id)
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("GradeHistory", id)));
        gradeHistoryRepo.deleteById(id);
    }

    @Override
    public EmployeeCostHistory updateCostHistory(UpdateCostHistoryCommand cmd) {
        EmployeeCostHistory existing = costHistoryRepo.findById(cmd.id())
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("CostHistory", cmd.id())));
        if (cmd.dailyCost() == null || cmd.dailyCost().signum() < 0)
            throw new DomainException(new DomainError.BusinessRule("Daily cost must be >= 0"));
        existing.setDailyCost(cmd.dailyCost());
        existing.setEffectiveFrom(cmd.effectiveFrom());
        return costHistoryRepo.save(existing);
    }

    @Override
    public void deleteCostHistory(UUID id) {
        costHistoryRepo.findById(id)
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("CostHistory", id)));
        costHistoryRepo.deleteById(id);
    }
}
