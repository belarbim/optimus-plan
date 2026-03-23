package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.RoleTypeConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoleTypeJpaRepository extends JpaRepository<RoleTypeConfigJpaEntity, UUID> {

    Optional<RoleTypeConfigJpaEntity> findByRoleType(String roleType);

    boolean existsByRoleType(String roleType);

    @Query("SELECT COUNT(rh) > 0 FROM RoleHistoryJpaEntity rh WHERE rh.roleType = (SELECT rt.roleType FROM RoleTypeConfigJpaEntity rt WHERE rt.id = :id)")
    boolean isReferencedInHistory(@Param("id") UUID id);
}
