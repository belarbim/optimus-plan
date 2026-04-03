package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;
import com.utmost.optimusplan.domain.model.EmployeeTypeHistory;
import com.utmost.optimusplan.domain.port.out.EmployeeTypeHistoryRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeTypeHistoryJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.EmployeeJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.EmployeeTypeHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class EmployeeTypeHistoryPersistenceAdapter implements EmployeeTypeHistoryRepositoryPort {
    private final EmployeeTypeHistoryJpaRepository repo;
    private final EmployeeJpaRepository employeeRepo;

    @Override
    public EmployeeTypeHistory save(EmployeeTypeHistory h) {
        EmployeeJpaEntity emp = employeeRepo.findById(h.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found: " + h.getEmployeeId()));
        EmployeeTypeHistoryJpaEntity entity = EmployeeTypeHistoryJpaEntity.builder()
                .id(h.getId() != null ? h.getId() : UUID.randomUUID())
                .employee(emp)
                .type(h.getType())
                .effectiveFrom(h.getEffectiveFrom())
                .build();
        return toDomain(repo.save(entity));
    }

    @Override
    public List<EmployeeTypeHistory> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId) {
        return repo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<EmployeeTypeHistory> findCurrentOnDate(UUID employeeId, LocalDate date) {
        return repo.findCurrentOnDate(employeeId, date).map(this::toDomain);
    }

    @Override
    public Optional<EmployeeTypeHistory> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    private EmployeeTypeHistory toDomain(EmployeeTypeHistoryJpaEntity e) {
        return EmployeeTypeHistory.builder()
                .id(e.getId())
                .employeeId(e.getEmployee().getId())
                .type(e.getType())
                .effectiveFrom(e.getEffectiveFrom())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
