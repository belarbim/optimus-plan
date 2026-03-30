package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface WorkingDaysUseCase {

    /**
     * Parses a CSV file containing columns "month" and "avgDaysWorked" and upserts
     * a {@link WorkingDaysConfig} row for each entry. Returns the persisted configs.
     */
    List<WorkingDaysConfig> importCsv(MultipartFile file);

    List<WorkingDaysConfig> findAll();

    /** Returns all configs whose month starts with the given year (yyyy). */
    List<WorkingDaysConfig> findByYear(int year);

    /** Upserts a single month entry. Month must be in yyyy-MM format. */
    WorkingDaysConfig upsertMonth(String month, BigDecimal avgDaysWorked);
}
