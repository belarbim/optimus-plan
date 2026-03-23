package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WorkingDaysUseCase {

    /**
     * Parses a CSV file containing columns "month" and "avgDaysWorked" and upserts
     * a {@link WorkingDaysConfig} row for each entry. Returns the persisted configs.
     */
    List<WorkingDaysConfig> importCsv(MultipartFile file);

    List<WorkingDaysConfig> findAll();
}
