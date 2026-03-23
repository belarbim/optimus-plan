package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepositoryPort {

    Employee save(Employee employee);

    Optional<Employee> findById(UUID id);

    List<Employee> findAll();

    void deleteById(UUID id);

    boolean existsByEmail(String email);

    Optional<Employee> findByEmail(String email);
}
