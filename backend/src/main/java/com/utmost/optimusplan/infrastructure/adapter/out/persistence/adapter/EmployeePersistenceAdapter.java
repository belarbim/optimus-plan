package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.Employee;
import com.utmost.optimusplan.domain.port.out.EmployeeRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.EmployeeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EmployeePersistenceAdapter implements EmployeeRepositoryPort {

    private final EmployeeJpaRepository repo;

    @Override
    public Employee save(Employee employee) {
        return toDomain(repo.save(toEntity(employee)));
    }

    @Override
    public Optional<Employee> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Employee> findAll() {
        return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repo.existsByEmail(email);
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        return repo.findByEmailIgnoreCase(email).map(this::toDomain);
    }

    private Employee toDomain(EmployeeJpaEntity e) {
        return Employee.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .type(e.getType())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private EmployeeJpaEntity toEntity(Employee employee) {
        EmployeeJpaEntity entity = employee.getId() != null
                ? repo.findById(employee.getId()).orElseGet(EmployeeJpaEntity::new)
                : new EmployeeJpaEntity();
        entity.setId(employee.getId());
        entity.setFirstName(employee.getFirstName());
        entity.setLastName(employee.getLastName());
        entity.setEmail(employee.getEmail());
        entity.setType(employee.getType() != null ? employee.getType() : "INTERNAL");
        return entity;
    }
}
