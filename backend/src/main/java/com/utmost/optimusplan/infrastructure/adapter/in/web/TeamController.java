package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.port.in.TeamUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamUseCase teamUseCase;

    record TeamRequest(@NotBlank String name, UUID parentId) {}

    record TeamResponse(UUID id, String name, UUID parentId, List<TeamResponse> children,
                        String createdAt, String updatedAt) {
        static TeamResponse from(Team t) {
            return new TeamResponse(
                    t.getId(),
                    t.getName(),
                    t.getParentId(),
                    t.getChildren() == null ? List.of() :
                            t.getChildren().stream().map(TeamResponse::from).toList(),
                    t.getCreatedAt() != null ? t.getCreatedAt().toString() : null,
                    t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@Valid @RequestBody TeamRequest req) {
        return TeamResponse.from(teamUseCase.create(new TeamUseCase.CreateTeamCommand(req.name(), req.parentId())));
    }

    @GetMapping
    public List<TeamResponse> getAll(@RequestParam(defaultValue = "true") boolean tree) {
        return teamUseCase.findAll(tree).stream().map(TeamResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TeamResponse getById(@PathVariable UUID id) {
        return TeamResponse.from(teamUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public TeamResponse update(@PathVariable UUID id, @Valid @RequestBody TeamRequest req) {
        return TeamResponse.from(teamUseCase.update(new TeamUseCase.UpdateTeamCommand(id, req.name())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        teamUseCase.delete(id);
    }
}
