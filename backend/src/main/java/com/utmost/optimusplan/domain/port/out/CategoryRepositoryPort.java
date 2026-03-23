package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.CategoryAllocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CategoryRepositoryPort {

    CategoryAllocation save(CategoryAllocation category);

    List<CategoryAllocation> findByTeamId(UUID teamId);

    void deleteByTeamId(UUID teamId);

    BigDecimal sumAllocationByTeamId(UUID teamId);
}
