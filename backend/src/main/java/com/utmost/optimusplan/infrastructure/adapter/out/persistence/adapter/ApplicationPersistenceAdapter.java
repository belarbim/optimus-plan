package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.Application;
import com.utmost.optimusplan.domain.port.out.ApplicationRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.ApplicationJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.ApplicationJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.TeamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApplicationPersistenceAdapter implements ApplicationRepositoryPort {

    private final ApplicationJpaRepository repo;
    private final TeamJpaRepository teamRepo;

    @Override
    public Application save(Application app) {
        ApplicationJpaEntity entity = app.getId() != null
                ? repo.findById(app.getId()).orElseGet(ApplicationJpaEntity::new)
                : new ApplicationJpaEntity();

        entity.setId(app.getId());
        entity.setName(app.getName());
        entity.setDescription(app.getDescription());
        entity.setTeam(app.getTeamId() != null
                ? teamRepo.findById(app.getTeamId()).orElse(null)
                : null);

        return toDomain(repo.save(entity));
    }

    @Override
    public Optional<Application> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Application> findAll() {
        return repo.findAllWithTeam().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Application> searchByName(String query) {
        return repo.searchByNameContaining(query).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        repo.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repo.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID id) {
        return repo.existsByNameAndIdNot(name, id);
    }

    private Application toDomain(ApplicationJpaEntity e) {
        return Application.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .teamId(e.getTeam() != null ? e.getTeam().getId() : null)
                .teamName(e.getTeam() != null ? e.getTeam().getName() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
