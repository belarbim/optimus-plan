package com.utmost.optimusplan.domain.port.out;
import com.utmost.optimusplan.domain.model.GradeCostHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeCostHistoryRepositoryPort {
    GradeCostHistory save(GradeCostHistory entry);
    List<GradeCostHistory> findByGradeIdOrderByEffectiveFromDesc(UUID gradeId);
    Optional<GradeCostHistory> findCurrentOnDate(UUID gradeId, LocalDate date);
    Optional<GradeCostHistory> findById(UUID id);
    void deleteById(UUID id);
}
