package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.RoleHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleHistoryJpaRepository extends JpaRepository<RoleHistoryJpaEntity, UUID> {

    List<RoleHistoryJpaEntity> findByAssignmentId(UUID assignmentId);

    List<RoleHistoryJpaEntity> findByAssignmentIdOrderByEffectiveFromAsc(UUID assignmentId);

    @Query("SELECT rh FROM RoleHistoryJpaEntity rh WHERE rh.assignment.id = :assignmentId AND rh.effectiveTo IS NULL")
    Optional<RoleHistoryJpaEntity> findCurrentByAssignmentId(@Param("assignmentId") UUID assignmentId);

    void deleteByAssignmentId(UUID assignmentId);
}
