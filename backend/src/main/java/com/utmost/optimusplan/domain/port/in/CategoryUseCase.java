package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.CategoryAllocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CategoryUseCase {

    record CategoryEntry(String categoryName, BigDecimal allocationPct) {}

    record SetCategoriesCommand(UUID teamId, List<CategoryEntry> categories) {}

    List<CategoryAllocation> findByTeam(UUID teamId);

    List<CategoryAllocation> setCategories(SetCategoriesCommand cmd);
}
