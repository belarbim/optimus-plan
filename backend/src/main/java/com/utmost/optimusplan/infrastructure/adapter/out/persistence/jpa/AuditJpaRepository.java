package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID>,
        JpaSpecificationExecutor<AuditLogJpaEntity> {

    List<AuditLogJpaEntity> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    @Query("""
            SELECT a FROM AuditLogJpaEntity a
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
            AND (:action IS NULL OR a.action = :action)
            AND (:dateFrom IS NULL OR a.timestamp >= :dateFrom)
            AND (:dateTo IS NULL OR a.timestamp <= :dateTo)
            ORDER BY a.timestamp DESC
            """)
    Page<AuditLogJpaEntity> findWithFilters(
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);
}
