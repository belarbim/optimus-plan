package com.utmost.optimusplan.application.gradecost;
import com.utmost.optimusplan.domain.model.GradeCostHistory;
import com.utmost.optimusplan.domain.port.in.GradeCostUseCase;
import com.utmost.optimusplan.domain.port.out.GradeCostHistoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.GradeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service @Transactional
public class GradeCostApplicationService implements GradeCostUseCase {
    private final GradeCostHistoryRepositoryPort costHistoryRepo;
    private final GradeRepositoryPort gradeRepo;

    public GradeCostApplicationService(GradeCostHistoryRepositoryPort costHistoryRepo, GradeRepositoryPort gradeRepo) {
        this.costHistoryRepo = costHistoryRepo;
        this.gradeRepo = gradeRepo;
    }

    @Override
    public GradeCostHistory addCostHistory(AddCostHistoryCommand cmd) {
        GradeCostHistory entry = GradeCostHistory.builder()
                .id(UUID.randomUUID())
                .gradeId(cmd.gradeId())
                .dailyCost(cmd.dailyCost())
                .effectiveFrom(cmd.effectiveFrom())
                .build();
        GradeCostHistory saved = costHistoryRepo.save(entry);

        // Update denormalized daily_cost on grade if this entry is now the current one
        Optional<GradeCostHistory> current = costHistoryRepo.findCurrentOnDate(cmd.gradeId(), LocalDate.now());
        if (current.isPresent() && current.get().getId().equals(saved.getId())) {
            gradeRepo.findById(cmd.gradeId()).ifPresent(g -> {
                g.setDailyCost(cmd.dailyCost());
                gradeRepo.save(g);
            });
        }

        return saved;
    }

    @Override @Transactional(readOnly = true)
    public List<GradeCostHistory> getCostHistory(UUID gradeId) {
        return costHistoryRepo.findByGradeIdOrderByEffectiveFromDesc(gradeId);
    }

    @Override @Transactional(readOnly = true)
    public Optional<GradeCostHistory> getCurrentCost(UUID gradeId) {
        return costHistoryRepo.findCurrentOnDate(gradeId, LocalDate.now());
    }

    @Override
    public GradeCostHistory updateCostHistory(UpdateCostHistoryCommand cmd) {
        GradeCostHistory existing = costHistoryRepo.findById(cmd.id())
                .orElseThrow(() -> new RuntimeException("Grade cost history entry not found: " + cmd.id()));
        existing.setDailyCost(cmd.dailyCost());
        existing.setEffectiveFrom(cmd.effectiveFrom());
        GradeCostHistory saved = costHistoryRepo.save(existing);

        // Update denormalized daily_cost on grade if this entry is now the current one
        Optional<GradeCostHistory> current = costHistoryRepo.findCurrentOnDate(existing.getGradeId(), LocalDate.now());
        if (current.isPresent() && current.get().getId().equals(saved.getId())) {
            gradeRepo.findById(existing.getGradeId()).ifPresent(g -> {
                g.setDailyCost(cmd.dailyCost());
                gradeRepo.save(g);
            });
        }

        return saved;
    }

    @Override
    public void deleteCostHistory(UUID id) {
        costHistoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Grade cost history entry not found: " + id));
        costHistoryRepo.deleteById(id);
    }
}
