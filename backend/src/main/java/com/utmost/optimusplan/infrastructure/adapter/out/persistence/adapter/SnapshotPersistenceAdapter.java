package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.utmost.optimusplan.domain.model.CapacitySnapshot;
import com.utmost.optimusplan.domain.port.out.SnapshotRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.CapacitySnapshotJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.TeamJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.SnapshotJpaRepository;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.TeamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SnapshotPersistenceAdapter implements SnapshotRepositoryPort {

    private final SnapshotJpaRepository repo;
    private final TeamJpaRepository teamRepo;

    @Override
    public List<CapacitySnapshot> saveAll(List<CapacitySnapshot> snapshots) {
        List<CapacitySnapshotJpaEntity> entities = snapshots.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        return repo.saveAll(entities).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<CapacitySnapshot> findByTeamIdAndMonthRange(UUID teamId, String from, String to) {
        return repo.findByTeamIdAndMonthRange(teamId, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByTeamIdAndMonth(UUID teamId, String month) {
        repo.deleteByTeamIdAndSnapshotMonth(teamId, month);
    }

    private CapacitySnapshot toDomain(CapacitySnapshotJpaEntity e) {
        return CapacitySnapshot.builder()
                .id(e.getId())
                .teamId(e.getTeam().getId())
                .snapshotMonth(e.getSnapshotMonth())
                .categoryName(e.getCategoryName())
                .capacityManDays(e.getCapacityManDays())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private CapacitySnapshotJpaEntity toEntity(CapacitySnapshot snapshot) {
        TeamJpaEntity team = teamRepo.findById(snapshot.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + snapshot.getTeamId()));
        return CapacitySnapshotJpaEntity.builder()
                .id(snapshot.getId())
                .team(team)
                .snapshotMonth(snapshot.getSnapshotMonth())
                .categoryName(snapshot.getCategoryName())
                .capacityManDays(snapshot.getCapacityManDays())
                .build();
    }
}
