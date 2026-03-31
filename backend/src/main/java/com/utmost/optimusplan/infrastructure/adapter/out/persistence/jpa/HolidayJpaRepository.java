package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.PublicHolidayJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HolidayJpaRepository extends JpaRepository<PublicHolidayJpaEntity, UUID> {

    boolean existsByDate(LocalDate date);

    List<PublicHolidayJpaEntity> findByDateBetween(LocalDate from, LocalDate to);
}
