package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;
import com.utmost.optimusplan.domain.model.Grade;
import com.utmost.optimusplan.domain.port.out.GradeRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.GradeJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.GradeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component @RequiredArgsConstructor
public class GradePersistenceAdapter implements GradeRepositoryPort {
    private final GradeJpaRepository repo;

    @Override public Grade save(Grade g) { return toDomain(repo.save(toEntity(g))); }
    @Override public Optional<Grade> findById(UUID id) { return repo.findById(id).map(this::toDomain); }
    @Override public List<Grade> findAll() { return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList()); }
    @Override public void deleteById(UUID id) { repo.deleteById(id); }
    @Override public boolean existsByName(String name) { return repo.existsByName(name); }
    @Override public boolean existsByNameAndIdNot(String name, UUID id) { return repo.existsByNameAndIdNot(name, id); }

    private Grade toDomain(GradeJpaEntity e) {
        return Grade.builder().id(e.getId()).name(e.getName()).dailyCost(e.getDailyCost())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build();
    }
    private GradeJpaEntity toEntity(Grade g) {
        GradeJpaEntity e = g.getId() != null ? repo.findById(g.getId()).orElseGet(GradeJpaEntity::new) : new GradeJpaEntity();
        e.setId(g.getId()); e.setName(g.getName()); e.setDailyCost(g.getDailyCost());
        return e;
    }
}
