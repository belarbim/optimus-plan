package com.utmost.optimusplan.application.application;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.Application;
import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.port.in.ApplicationUseCase;
import com.utmost.optimusplan.domain.port.out.ApplicationRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ApplicationApplicationService implements ApplicationUseCase {

    private final ApplicationRepositoryPort repo;
    private final TeamRepositoryPort teamRepo;

    public ApplicationApplicationService(ApplicationRepositoryPort repo, TeamRepositoryPort teamRepo) {
        this.repo = repo;
        this.teamRepo = teamRepo;
    }

    @Override
    public Application create(CreateApplicationCommand cmd) {
        if (repo.existsByName(cmd.name())) {
            throw new DomainException(new DomainError.Conflict(
                    "Application '" + cmd.name() + "' already exists"));
        }
        if (cmd.teamId() != null && !teamRepo.existsById(cmd.teamId())) {
            throw new DomainException(new DomainError.NotFound("Team", cmd.teamId()));
        }
        LocalDateTime now = LocalDateTime.now();
        Application app = Application.builder()
                .id(UUID.randomUUID())
                .name(cmd.name())
                .description(cmd.description())
                .teamId(cmd.teamId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return repo.save(app);
    }

    @Override
    public Application update(UpdateApplicationCommand cmd) {
        Application app = findById(cmd.id());
        if (!app.getName().equals(cmd.name()) && repo.existsByNameAndIdNot(cmd.name(), cmd.id())) {
            throw new DomainException(new DomainError.Conflict(
                    "Application '" + cmd.name() + "' already exists"));
        }
        if (cmd.teamId() != null && !teamRepo.existsById(cmd.teamId())) {
            throw new DomainException(new DomainError.NotFound("Team", cmd.teamId()));
        }
        app.setName(cmd.name());
        app.setDescription(cmd.description());
        app.setTeamId(cmd.teamId());
        app.setUpdatedAt(LocalDateTime.now());
        return repo.save(app);
    }

    @Override
    public void delete(UUID id) {
        findById(id);
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Application findById(UUID id) {
        return repo.findById(id).orElseThrow(() ->
                new DomainException(new DomainError.NotFound("Application", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> findAll() {
        return repo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Application> search(String query) {
        if (query == null || query.isBlank()) {
            return repo.findAll();
        }
        return repo.searchByName(query.trim());
    }

    @Override
    public ImportResult importBatch(List<ImportApplicationRow> rows) {
        int success = 0;
        int errors  = 0;
        List<ImportRowError> errorList = new ArrayList<>();

        for (ImportApplicationRow row : rows) {
            try {
                if (row.name() == null || row.name().isBlank()) {
                    errorList.add(new ImportRowError(row.rowNumber(), "", "Name is required"));
                    errors++;
                    continue;
                }
                if (repo.existsByName(row.name().trim())) {
                    errorList.add(new ImportRowError(row.rowNumber(), row.name(),
                            "Application '" + row.name() + "' already exists (skipped)"));
                    errors++;
                    continue;
                }

                UUID teamId = null;
                if (row.teamName() != null && !row.teamName().isBlank()) {
                    Optional<Team> team = teamRepo.findByNameIgnoreCase(row.teamName().trim());
                    if (team.isPresent()) {
                        teamId = team.get().getId();
                    } else {
                        errorList.add(new ImportRowError(row.rowNumber(), row.name(),
                                "Team '" + row.teamName() + "' not found — application created without team"));
                    }
                }

                LocalDateTime now = LocalDateTime.now();
                repo.save(Application.builder()
                        .id(UUID.randomUUID())
                        .name(row.name().trim())
                        .description(row.description() != null && !row.description().isBlank()
                                ? row.description().trim() : null)
                        .teamId(teamId)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
                success++;
            } catch (Exception ex) {
                errorList.add(new ImportRowError(row.rowNumber(), row.name(), ex.getMessage()));
                errors++;
            }
        }
        return new ImportResult(success, errors, errorList);
    }
}
