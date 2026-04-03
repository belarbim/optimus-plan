package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeCostHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface EmployeeCostHistoryJpaRepository extends JpaRepository<EmployeeCostHistoryJpaEntity, UUID> {
    List<EmployeeCostHistoryJpaEntity> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);
    @Query("SELECT h FROM EmployeeCostHistoryJpaEntity h WHERE h.employeeId = :eid AND h.effectiveFrom <= :date ORDER BY h.effectiveFrom DESC")
    List<EmployeeCostHistoryJpaEntity> findCurrentOnDate(@Param("eid") UUID employeeId, @Param("date") LocalDate date);
}
