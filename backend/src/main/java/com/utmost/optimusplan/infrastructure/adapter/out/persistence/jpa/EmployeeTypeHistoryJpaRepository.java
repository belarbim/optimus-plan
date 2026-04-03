package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeTypeHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeTypeHistoryJpaRepository extends JpaRepository<EmployeeTypeHistoryJpaEntity, UUID> {
    List<EmployeeTypeHistoryJpaEntity> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);

    @Query("SELECT h FROM EmployeeTypeHistoryJpaEntity h WHERE h.employee.id = :employeeId AND h.effectiveFrom <= :date ORDER BY h.effectiveFrom DESC LIMIT 1")
    Optional<EmployeeTypeHistoryJpaEntity> findCurrentOnDate(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date);
}
