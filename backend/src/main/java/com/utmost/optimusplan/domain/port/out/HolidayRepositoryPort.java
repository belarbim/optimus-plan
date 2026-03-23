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

    /**
     * Returns holidays that fall within the given month (yyyy-MM) and match the locale.
     */
    List<PublicHoliday> findByMonthAndLocale(String month, String locale);

    List<PublicHoliday> findRecurring();

    void deleteById(UUID id);

    boolean existsByDateAndLocale(LocalDate date, String locale);
}
