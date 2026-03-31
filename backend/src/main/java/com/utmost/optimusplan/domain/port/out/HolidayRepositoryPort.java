package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.PublicHoliday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HolidayRepositoryPort {

    PublicHoliday save(PublicHoliday holiday);

    Optional<PublicHoliday> findById(UUID id);

    List<PublicHoliday> findAll();

    /** Returns holidays that fall within the given month (yyyy-MM). */
    List<PublicHoliday> findByMonth(String month);

    /** Returns holidays that fall within the given calendar year. */
    List<PublicHoliday> findByYear(int year);

    void deleteById(UUID id);

    boolean existsByDate(LocalDate date);
}
