package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.TeamType;
import com.utmost.optimusplan.domain.model.TeamTypeCategory;
import com.utmost.optimusplan.domain.port.in.TeamTypeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/team-types")
@RequiredArgsConstructor
public class TeamTypeController {

    private final TeamTypeUseCase teamTypeUseCase;

    record CategoryDefinitionRequest(
            @NotBlank String name,
            @NotNull Boolean isPartOfTotalCapacity,
            @NotNull Boolean isPartOfRemainingCapacity) {}

    record TeamTypeRequest(
            @NotBlank String name,
            @NotEmpty List<CategoryDefinitionRequest> categories) {}

    record TeamTypeCategoryResponse(
            UUID    id,
            String  name,
            boolean isPartOfTotalCapacity,
            boolean isPartOfRemainingCapacity) {

        static TeamTypeCategoryResponse from(TeamTypeCategory c) {
            return new TeamTypeCategoryResponse(
                    c.getId(), c.getName(),
                    c.isPartOfTotalCapacity(), c.isPartOfRemainingCapacity());
        }
    }

    record TeamTypeResponse(
            UUID   id,
            String name,
            List<TeamTypeCategoryResponse> categories) {

        static TeamTypeResponse from(TeamType t) {
            return new TeamTypeResponse(
                    t.getId(),
                    t.getName(),
                    t.getCategories().stream().map(TeamTypeCategoryResponse::from).toList());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamTypeResponse create(@Valid @RequestBody TeamTypeRequest req) {
        return TeamTypeResponse.from(
                teamTypeUseCase.create(new TeamTypeUseCase.CreateTeamTypeCommand(
                        req.name(), toDefs(req.categories()))));
    }

    @PutMapping("/{id}")
    public TeamTypeResponse update(@PathVariable UUID id, @Valid @RequestBody TeamTypeRequest req) {
        return TeamTypeResponse.from(
                teamTypeUseCase.update(new TeamTypeUseCase.UpdateTeamTypeCommand(
                        id, req.name(), toDefs(req.categories()))));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        teamTypeUseCase.delete(id);
    }

    @GetMapping
    public List<TeamTypeResponse> findAll() {
        return teamTypeUseCase.findAll().stream().map(TeamTypeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TeamTypeResponse findById(@PathVariable UUID id) {
        return TeamTypeResponse.from(teamTypeUseCase.findById(id));
    }

    private List<TeamTypeUseCase.CategoryDefinition> toDefs(List<CategoryDefinitionRequest> reqs) {
        return reqs.stream()
                .map(r -> new TeamTypeUseCase.CategoryDefinition(
                        r.name(), r.isPartOfTotalCapacity(), r.isPartOfRemainingCapacity()))
                .toList();
    }
}
