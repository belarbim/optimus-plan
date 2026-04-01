package com.utmost.optimusplan.application.teamtype;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.TeamType;
import com.utmost.optimusplan.domain.model.TeamTypeCategory;
import com.utmost.optimusplan.domain.port.in.TeamTypeUseCase;
import com.utmost.optimusplan.domain.port.out.TeamTypeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamTypeApplicationService implements TeamTypeUseCase {

    private final TeamTypeRepositoryPort teamTypeRepo;

    public TeamTypeApplicationService(TeamTypeRepositoryPort teamTypeRepo) {
        this.teamTypeRepo = teamTypeRepo;
    }

    @Override
    public TeamType create(CreateTeamTypeCommand cmd) {
        if (teamTypeRepo.existsByName(cmd.name())) {
            throw new DomainException(new DomainError.Conflict(
                    "A team type named '" + cmd.name() + "' already exists"));
        }
        validateCategories(cmd.categories());

        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        TeamType teamType = TeamType.builder()
                .id(id)
                .name(cmd.name())
                .categories(toCategories(id, cmd.categories()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return teamTypeRepo.save(teamType);
    }

    @Override
    public TeamType update(UpdateTeamTypeCommand cmd) {
        TeamType existing = findById(cmd.id());
        if (!existing.getName().equals(cmd.name()) && teamTypeRepo.existsByName(cmd.name())) {
            throw new DomainException(new DomainError.Conflict(
                    "A team type named '" + cmd.name() + "' already exists"));
        }
        validateCategories(cmd.categories());

        existing.setName(cmd.name());
        existing.setCategories(toCategories(cmd.id(), cmd.categories()));
        existing.setUpdatedAt(LocalDateTime.now());
        return teamTypeRepo.save(existing);
    }

    @Override
    public void delete(UUID id) {
        findById(id);
        teamTypeRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamType findById(UUID id) {
        return teamTypeRepo.findById(id)
                .orElseThrow(() -> new DomainException(new DomainError.NotFound("TeamType", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamType> findAll() {
        return teamTypeRepo.findAll();
    }

    // -------------------------------------------------------------------------

    private void validateCategories(List<CategoryDefinition> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new DomainException(new DomainError.BusinessRule(
                    "A team type must have at least one category"));
        }
        for (CategoryDefinition cat : categories) {
            boolean total     = cat.isPartOfTotalCapacity();
            boolean remaining = cat.isPartOfRemainingCapacity();
            if (total == remaining) { // both true or both false
                throw new DomainException(new DomainError.BusinessRule(
                        "Category '" + cat.name() + "' must be either part of total capacity " +
                        "or part of remaining capacity, not both or neither"));
            }
        }
    }

    private List<TeamTypeCategory> toCategories(UUID teamTypeId, List<CategoryDefinition> defs) {
        return defs.stream()
                .map(d -> TeamTypeCategory.builder()
                        .id(UUID.randomUUID())
                        .teamTypeId(teamTypeId)
                        .name(d.name())
                        .isPartOfTotalCapacity(d.isPartOfTotalCapacity())
                        .isPartOfRemainingCapacity(d.isPartOfRemainingCapacity())
                        .build())
                .collect(Collectors.toList());
    }
}
