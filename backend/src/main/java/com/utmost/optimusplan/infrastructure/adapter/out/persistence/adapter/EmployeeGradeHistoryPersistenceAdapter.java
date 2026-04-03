package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;
import com.utmost.optimusplan.domain.model.EmployeeGradeHistory;
import com.utmost.optimusplan.domain.port.out.EmployeeGradeHistoryRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeGradeHistoryJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.GradeJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.EmployeeGradeHistoryJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.GradeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class EmployeeGradeHistoryPersistenceAdapter implements EmployeeGradeHistoryRepositoryPort {
    private final EmployeeGradeHistoryJpaRepository repo;
    private final GradeJpaRepository gradeRepo;

    @Override
    public EmployeeGradeHistory save(EmployeeGradeHistory h) {
        GradeJpaEntity grade = gradeRepo.findById(h.getGradeId())
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + h.getGradeId()));
        EmployeeGradeHistoryJpaEntity e = EmployeeGradeHistoryJpaEntity.builder()
                .id(h.getId()).employeeId(h.getEmployeeId()).grade(grade)
                .effectiveFrom(h.getEffectiveFrom()).createdAt(h.getCreatedAt()).build();
        return toDomain(repo.save(e));
    }

    @Override
    public List<EmployeeGradeHistory> findByEmployeeId(UUID employeeId) {
        return repo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<EmployeeGradeHistory> findCurrentOnDate(UUID employeeId, LocalDate date) {
        return repo.findCurrentOnDate(employeeId, date).stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<EmployeeGradeHistory> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    private EmployeeGradeHistory toDomain(EmployeeGradeHistoryJpaEntity e) {
        return EmployeeGradeHistory.builder()
                .id(e.getId()).employeeId(e.getEmployeeId()).gradeId(e.getGrade().getId())
                .gradeName(e.getGrade().getName()).dailyCost(e.getGrade().getDailyCost())
                .effectiveFrom(e.getEffectiveFrom()).createdAt(e.getCreatedAt()).build();
    }
}
