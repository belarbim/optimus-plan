package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeJpaEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<EmployeeJpaEntity> findByEmailIgnoreCase(String email);
}
