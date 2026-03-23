package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import com.utmost.optimusplan.domain.port.out.WorkingDaysRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.WorkingDaysConfigJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.WorkingDaysJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkingDaysPersistenceAdapter implements WorkingDaysRepositoryPort {

    private final WorkingDaysJpaRepository repo;

    @Override
    public WorkingDaysConfig save(WorkingDaysConfig config) {
        return toDomain(repo.save(toEntity(config)));
    }

    @Override
    public List<WorkingDaysConfig> findAll() {
        return repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<WorkingDaysConfig> findByMonth(String month) {
        return repo.findByMonth(month).map(this::toDomain);
    }

    private WorkingDaysConfig toDomain(WorkingDaysConfigJpaEntity e) {
        return WorkingDaysConfig.builder()
                .id(e.getId())
                .month(e.getMonth())
                .avgDaysWorked(e.getAvgDaysWorked())
                .importedAt(e.getImportedAt())
                .build();
    }

    private WorkingDaysConfigJpaEntity toEntity(WorkingDaysConfig config) {
        WorkingDaysConfigJpaEntity entity = config.getId() != null
                ? repo.findById(config.getId()).orElseGet(WorkingDaysConfigJpaEntity::new)
                : new WorkingDaysConfigJpaEntity();
        entity.setId(config.getId());
        entity.setMonth(config.getMonth());
        entity.setAvgDaysWorked(config.getAvgDaysWorked());
        return entity;
    }
}
