package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.WorkingDaysConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkingDaysJpaRepository extends JpaRepository<WorkingDaysConfigJpaEntity, UUID> {

    Optional<WorkingDaysConfigJpaEntity> findByMonth(String month);

    List<WorkingDaysConfigJpaEntity> findByMonthStartingWith(String yearPrefix);
}
