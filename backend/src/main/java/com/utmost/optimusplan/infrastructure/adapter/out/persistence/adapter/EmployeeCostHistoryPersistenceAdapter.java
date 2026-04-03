package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;
import com.utmost.optimusplan.domain.model.EmployeeCostHistory;
import com.utmost.optimusplan.domain.port.out.EmployeeCostHistoryRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeCostHistoryJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.EmployeeCostHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class EmployeeCostHistoryPersistenceAdapter implements EmployeeCostHistoryRepositoryPort {
    private final EmployeeCostHistoryJpaRepository repo;

    @Override
    public EmployeeCostHistory save(EmployeeCostHistory h) {
        EmployeeCostHistoryJpaEntity e = EmployeeCostHistoryJpaEntity.builder()
                .id(h.getId()).employeeId(h.getEmployeeId()).dailyCost(h.getDailyCost())
                .effectiveFrom(h.getEffectiveFrom()).createdAt(h.getCreatedAt()).build();
        return toDomain(repo.save(e));
    }

    @Override
    public List<EmployeeCostHistory> findByEmployeeId(UUID employeeId) {
        return repo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<EmployeeCostHistory> findCurrentOnDate(UUID employeeId, LocalDate date) {
        return repo.findCurrentOnDate(employeeId, date).stream().findFirst().map(this::toDomain);
    }

    @Override
    public Optional<EmployeeCostHistory> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    private EmployeeCostHistory toDomain(EmployeeCostHistoryJpaEntity e) {
        return EmployeeCostHistory.builder().id(e.getId()).employeeId(e.getEmployeeId())
                .dailyCost(e.getDailyCost()).effectiveFrom(e.getEffectiveFrom()).createdAt(e.getCreatedAt()).build();
    }
}
