package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.CategoryAllocationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryAllocationJpaEntity, UUID> {

    List<CategoryAllocationJpaEntity> findByTeamId(UUID teamId);

    @Modifying
    @Transactional
    void deleteByTeamId(UUID teamId);

    @Query("SELECT COALESCE(SUM(ca.allocationPct), 0) FROM CategoryAllocationJpaEntity ca WHERE ca.team.id = :teamId")
    BigDecimal sumAllocationByTeamId(@Param("teamId") UUID teamId);
}
