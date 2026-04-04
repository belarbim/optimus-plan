package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.utmost.optimusplan.domain.model.Application;
import com.utmost.optimusplan.domain.port.in.ApplicationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationUseCase applicationUseCase;

    record CreateApplicationRequest(
            @NotBlank String name,
            String description,
            UUID teamId) {}

    record UpdateApplicationRequest(
            @NotBlank String name,
            String description,
            UUID teamId) {}

    record ApplicationResponse(
            UUID id,
            String name,
            String description,
            UUID teamId,
            String teamName,
            String createdAt) {

        static ApplicationResponse from(Application a) {
            return new ApplicationResponse(
                    a.getId(),
                    a.getName(),
                    a.getDescription(),
                    a.getTeamId(),
                    a.getTeamName(),
                    a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        }
    }

    @GetMapping
    public List<ApplicationResponse> getAll(@RequestParam(required = false) String search) {
        List<Application> results = (search != null && !search.isBlank())
                ? applicationUseCase.search(search)
                : applicationUseCase.findAll();
        return results.stream().map(ApplicationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable UUID id) {
        return ApplicationResponse.from(applicationUseCase.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody CreateApplicationRequest req) {
        return ApplicationResponse.from(applicationUseCase.create(
                new ApplicationUseCase.CreateApplicationCommand(req.name(), req.description(), req.teamId())));
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateApplicationRequest req) {
        return ApplicationResponse.from(applicationUseCase.update(
                new ApplicationUseCase.UpdateApplicationCommand(id, req.name(), req.description(), req.teamId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        applicationUseCase.delete(id);
    }

    // ── CSV Import ────────────────────────────────────────────────────────────

    record ImportResultResponse(int successCount, int errorCount, List<String> errors) {}

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultResponse importCsv(@RequestParam("file") MultipartFile file)
            throws IOException, CsvException {

        List<ApplicationUseCase.ImportApplicationRow> rows = new ArrayList<>();

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            List<String[]> all = reader.readAll();
            // Skip header row
            for (int i = 1; i < all.size(); i++) {
                String[] cols = all.get(i);
                if (cols.length < 1) continue;
                String name        = cols[0].trim();
                String description = cols.length > 1 ? cols[1].trim() : "";
                String teamName    = cols.length > 2 ? cols[2].trim() : "";
                rows.add(new ApplicationUseCase.ImportApplicationRow(
                        i + 1, name, description, teamName));
            }
        }

        ApplicationUseCase.ImportResult result = applicationUseCase.importBatch(rows);
        List<String> errorMessages = result.errors().stream()
                .map(e -> "Row " + e.row() + " [" + e.name() + "]: " + e.reason())
                .toList();
        return new ImportResultResponse(result.successCount(), result.errorCount(), errorMessages);
    }
}
