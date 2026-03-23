package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.CategoryAllocation;
import com.utmost.optimusplan.domain.port.in.CategoryUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams/{teamId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    record CategoryEntryRequest(
            @NotBlank String categoryName,
            @NotNull BigDecimal allocationPct) {}

    record SetCategoriesRequest(List<CategoryEntryRequest> categories) {}

    record CategoryAllocationResponse(
            UUID id,
            UUID teamId,
            String categoryName,
            BigDecimal allocationPct) {

        static CategoryAllocationResponse from(CategoryAllocation c) {
            return new CategoryAllocationResponse(c.getId(), c.getTeamId(), c.getCategoryName(), c.getAllocationPct());
        }
    }

    @GetMapping
    public List<CategoryAllocationResponse> getCategories(@PathVariable UUID teamId) {
        return categoryUseCase.findByTeam(teamId).stream()
                .map(CategoryAllocationResponse::from)
                .toList();
    }

    @PutMapping
    public List<CategoryAllocationResponse> setCategories(
            @PathVariable UUID teamId,
            @Valid @RequestBody SetCategoriesRequest req) {
        List<CategoryUseCase.CategoryEntry> entries = req.categories().stream()
                .map(c -> new CategoryUseCase.CategoryEntry(c.categoryName(), c.allocationPct()))
                .toList();
        return categoryUseCase.setCategories(new CategoryUseCase.SetCategoriesCommand(teamId, entries))
                .stream()
                .map(CategoryAllocationResponse::from)
                .toList();
    }
}
