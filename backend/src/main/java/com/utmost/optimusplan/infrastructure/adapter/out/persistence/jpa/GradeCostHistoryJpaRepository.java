package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.GradeCostHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeCostHistoryJpaRepository extends JpaRepository<GradeCostHistoryJpaEntity, UUID> {
    List<GradeCostHistoryJpaEntity> findByGradeIdOrderByEffectiveFromDesc(UUID gradeId);

    @Query("SELECT h FROM GradeCostHistoryJpaEntity h WHERE h.grade.id = :gradeId AND h.effectiveFrom <= :date ORDER BY h.effectiveFrom DESC LIMIT 1")
    Optional<GradeCostHistoryJpaEntity> findCurrentOnDate(@Param("gradeId") UUID gradeId, @Param("date") LocalDate date);
}
