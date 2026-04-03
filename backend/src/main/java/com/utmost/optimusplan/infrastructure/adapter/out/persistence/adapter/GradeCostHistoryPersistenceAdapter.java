package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;
import com.utmost.optimusplan.domain.model.GradeCostHistory;
import com.utmost.optimusplan.domain.port.out.GradeCostHistoryRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.GradeCostHistoryJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.GradeJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.GradeCostHistoryJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.GradeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class GradeCostHistoryPersistenceAdapter implements GradeCostHistoryRepositoryPort {
    private final GradeCostHistoryJpaRepository repo;
    private final GradeJpaRepository gradeRepo;

    @Override
    public GradeCostHistory save(GradeCostHistory h) {
        GradeJpaEntity grade = gradeRepo.findById(h.getGradeId())
                .orElseThrow(() -> new RuntimeException("Grade not found: " + h.getGradeId()));
        GradeCostHistoryJpaEntity entity = GradeCostHistoryJpaEntity.builder()
                .id(h.getId() != null ? h.getId() : UUID.randomUUID())
                .grade(grade)
                .dailyCost(h.getDailyCost())
                .effectiveFrom(h.getEffectiveFrom())
                .build();
        return toDomain(repo.save(entity));
    }

    @Override
    public List<GradeCostHistory> findByGradeIdOrderByEffectiveFromDesc(UUID gradeId) {
        return repo.findByGradeIdOrderByEffectiveFromDesc(gradeId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<GradeCostHistory> findCurrentOnDate(UUID gradeId, LocalDate date) {
        return repo.findCurrentOnDate(gradeId, date).map(this::toDomain);
    }

    @Override
    public Optional<GradeCostHistory> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    private GradeCostHistory toDomain(GradeCostHistoryJpaEntity e) {
        return GradeCostHistory.builder()
                .id(e.getId())
                .gradeId(e.getGrade().getId())
                .dailyCost(e.getDailyCost())
                .effectiveFrom(e.getEffectiveFrom())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
