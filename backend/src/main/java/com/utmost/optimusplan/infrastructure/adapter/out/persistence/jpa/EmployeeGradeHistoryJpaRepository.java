package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.EmployeeGradeHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface EmployeeGradeHistoryJpaRepository extends JpaRepository<EmployeeGradeHistoryJpaEntity, UUID> {
    List<EmployeeGradeHistoryJpaEntity> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);
    @Query("SELECT h FROM EmployeeGradeHistoryJpaEntity h WHERE h.employeeId = :eid AND h.effectiveFrom <= :date ORDER BY h.effectiveFrom DESC")
    List<EmployeeGradeHistoryJpaEntity> findCurrentOnDate(@Param("eid") UUID employeeId, @Param("date") LocalDate date);
}
