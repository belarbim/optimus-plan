package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.port.in.TeamUseCase;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamTypeRepositoryPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamUseCase            teamUseCase;
    private final TeamRepositoryPort     teamRepo;
    private final TeamTypeRepositoryPort teamTypeRepo;

    record TeamRequest(@NotBlank String name, UUID parentId, UUID teamTypeId) {}

    record TeamResponse(UUID id, String name, UUID parentId, UUID teamTypeId,
                        List<TeamResponse> children, String createdAt, String updatedAt) {
        static TeamResponse from(Team t) {
            return new TeamResponse(
                    t.getId(),
                    t.getName(),
                    t.getParentId(),
                    t.getTeamTypeId(),
                    t.getChildren() == null ? List.of() :
                            t.getChildren().stream().map(TeamResponse::from).toList(),
                    t.getCreatedAt() != null ? t.getCreatedAt().toString() : null,
                    t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@Valid @RequestBody TeamRequest req) {
        return TeamResponse.from(teamUseCase.create(
                new TeamUseCase.CreateTeamCommand(req.name(), req.parentId(), req.teamTypeId())));
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
        return TeamResponse.from(teamUseCase.update(new TeamUseCase.UpdateTeamCommand(id, req.name(), req.teamTypeId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        teamUseCase.delete(id);
    }

    // ── CSV Import ───────────────────────────────────────────────────────────

    record ImportResultResponse(int successCount, int errorCount, List<String> errors) {}

    @PostMapping("/import")
    public ImportResultResponse importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        List<String[]> rows;
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            rows = reader.readAll();
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV: " + e.getMessage(), e);
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        // Skip header row
        int start = (rows.size() > 0 && rows.get(0).length > 0
                && rows.get(0)[0].trim().equalsIgnoreCase("name")) ? 1 : 0;

        for (int i = start; i < rows.size(); i++) {
            int rowNum = i + 1;
            String[] cols = rows.get(i);
            if (cols.length == 0 || cols[0].isBlank()) continue;

            String name           = cols[0].trim();
            String parentName     = cols.length > 1 ? cols[1].trim() : "";
            String teamTypeName   = cols.length > 2 ? cols[2].trim() : "";

            try {
                UUID parentId = null;
                if (!parentName.isEmpty()) {
                    parentId = teamRepo.findByNameIgnoreCase(parentName)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Parent team '" + parentName + "' not found"))
                            .getId();
                }

                UUID teamTypeId = null;
                if (!teamTypeName.isEmpty() && parentId == null) {
                    teamTypeId = teamTypeRepo.findByNameIgnoreCase(teamTypeName)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Team type '" + teamTypeName + "' not found"))
                            .getId();
                }

                teamUseCase.create(new TeamUseCase.CreateTeamCommand(name, parentId, teamTypeId));
                successCount++;
            } catch (Exception e) {
                errors.add("Row " + rowNum + " (" + name + "): " + e.getMessage());
            }
        }

        return new ImportResultResponse(successCount, errors.size(), errors);
    }
}
