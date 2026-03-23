package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.CapacitySnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SnapshotJpaRepository extends JpaRepository<CapacitySnapshotJpaEntity, UUID> {

    @Query("""
            SELECT s FROM CapacitySnapshotJpaEntity s
            WHERE s.team.id = :teamId
            AND s.snapshotMonth >= :from
            AND s.snapshotMonth <= :to
            ORDER BY s.snapshotMonth ASC
            """)
    List<CapacitySnapshotJpaEntity> findByTeamIdAndMonthRange(
            @Param("teamId") UUID teamId,
            @Param("from") String from,
            @Param("to") String to);

    @Modifying
    @Transactional
    void deleteByTeamIdAndSnapshotMonth(UUID teamId, String snapshotMonth);
}
