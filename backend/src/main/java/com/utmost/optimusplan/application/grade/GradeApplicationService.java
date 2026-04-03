package com.utmost.optimusplan.application.grade;
import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.Grade;
import com.utmost.optimusplan.domain.model.GradeCostHistory;
import com.utmost.optimusplan.domain.port.in.GradeUseCase;
import com.utmost.optimusplan.domain.port.out.GradeCostHistoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.GradeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service @Transactional
public class GradeApplicationService implements GradeUseCase {
    private final GradeRepositoryPort repo;
    private final GradeCostHistoryRepositoryPort costHistoryRepo;

    public GradeApplicationService(GradeRepositoryPort repo, GradeCostHistoryRepositoryPort costHistoryRepo) {
        this.repo = repo;
        this.costHistoryRepo = costHistoryRepo;
    }

    @Override
    public Grade create(CreateGradeCommand cmd) {
        if (repo.existsByName(cmd.name()))
            throw new DomainException(new DomainError.Conflict("Grade '" + cmd.name() + "' already exists"));
        LocalDateTime now = LocalDateTime.now();
        Grade saved = repo.save(Grade.builder().id(UUID.randomUUID()).name(cmd.name()).dailyCost(cmd.dailyCost()).createdAt(now).updatedAt(now).build());
        // Create initial cost history entry
        costHistoryRepo.save(GradeCostHistory.builder()
                .id(UUID.randomUUID())
                .gradeId(saved.getId())
                .dailyCost(cmd.dailyCost())
                .effectiveFrom(cmd.effectiveFrom() != null ? cmd.effectiveFrom() : LocalDate.now())
                .build());
        return saved;
    }

    @Override
    public Grade update(UpdateGradeCommand cmd) {
        Grade g = findById(cmd.id());
        if (!g.getName().equals(cmd.name()) && repo.existsByNameAndIdNot(cmd.name(), cmd.id()))
            throw new DomainException(new DomainError.Conflict("Grade '" + cmd.name() + "' already exists"));
        g.setName(cmd.name());
        g.setUpdatedAt(LocalDateTime.now());
        return repo.save(g);
    }

    @Override
    public void delete(UUID id) { findById(id); repo.deleteById(id); }

    @Override @Transactional(readOnly = true)
    public Grade findById(UUID id) {
        return repo.findById(id).orElseThrow(() -> new DomainException(new DomainError.NotFound("Grade", id)));
    }

    @Override @Transactional(readOnly = true)
    public List<Grade> findAll() { return repo.findAll(); }
}
