package com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa;

import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.PublicHolidayJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HolidayJpaRepository extends JpaRepository<PublicHolidayJpaEntity, UUID> {

    List<PublicHolidayJpaEntity> findByRecurringTrue();

    boolean existsByDateAndLocale(LocalDate date, String locale);

    @Query("SELECT h FROM PublicHolidayJpaEntity h WHERE h.locale = :locale AND FUNCTION('TO_CHAR', h.date, 'YYYY-MM') = :month")
    List<PublicHolidayJpaEntity> findByMonthAndLocale(@Param("month") String month, @Param("locale") String locale);
}
