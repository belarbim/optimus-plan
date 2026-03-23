package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.PublicHoliday;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HolidayUseCase {

    record CreateHolidayCommand(
            LocalDate date,
            String name,
            String locale,
            boolean recurring) {}

    record UpdateHolidayCommand(
            UUID id,
            LocalDate date,
            String name,
            String locale,
            boolean recurring) {}

    List<PublicHoliday> findAll(String month, String locale);

    PublicHoliday findById(UUID id);

    PublicHoliday create(CreateHolidayCommand cmd);

    PublicHoliday update(UpdateHolidayCommand cmd);

    void delete(UUID id);
}
