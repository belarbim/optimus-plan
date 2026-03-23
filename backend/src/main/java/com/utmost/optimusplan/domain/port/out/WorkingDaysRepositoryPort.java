package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.WorkingDaysConfig;

import java.util.List;
import java.util.Optional;

public interface WorkingDaysRepositoryPort {

    WorkingDaysConfig save(WorkingDaysConfig config);

    List<WorkingDaysConfig> findAll();

    Optional<WorkingDaysConfig> findByMonth(String month);
}
