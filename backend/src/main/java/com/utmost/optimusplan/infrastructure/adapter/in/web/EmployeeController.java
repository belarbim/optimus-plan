package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.Employee;
import com.utmost.optimusplan.domain.port.in.AssignmentUseCase;
import com.utmost.optimusplan.domain.port.in.EmployeeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeUseCase employeeUseCase;
    private final AssignmentUseCase assignmentUseCase;

    record CreateEmployeeRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Email @NotBlank String email) {}

    record UpdateEmployeeRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Email @NotBlank String email) {}

    record AssignmentResponse(
            UUID id,
            UUID teamId,
            String teamName,
            UUID employeeId,
            String employeeName,
            BigDecimal allocationPct,
            String roleType,
            BigDecimal roleWeight,
            String startDate,
            String endDate) {}

    record EmployeeResponse(
            UUID id,
            String firstName,
            String lastName,
            String email,
            BigDecimal totalAllocation,
            List<AssignmentResponse> assignments,
            String createdAt) {

        static EmployeeResponse from(Employee employee, List<AssignmentResponse> assignments) {
            BigDecimal totalAllocation = assignments.stream()
                    .map(AssignmentResponse::allocationPct)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new EmployeeResponse(
                    employee.getId(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getEmail(),
                    totalAllocation,
                    assignments,
                    employee.getCreatedAt() != null ? employee.getCreatedAt().toString() : null);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest req) {
        Employee employee = employeeUseCase.create(
                new EmployeeUseCase.CreateEmployeeCommand(req.firstName(), req.lastName(), req.email()));
        return buildResponse(employee);
    }

    @GetMapping
    public List<EmployeeResponse> getAll() {
        return employeeUseCase.findAll().stream()
                .map(this::buildResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable UUID id) {
        return buildResponse(employeeUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateEmployeeRequest req) {
        Employee employee = employeeUseCase.update(
                new EmployeeUseCase.UpdateEmployeeCommand(id, req.firstName(), req.lastName(), req.email()));
        return buildResponse(employee);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        employeeUseCase.delete(id);
    }

    // ── CSV Import ────────────────────────────────────────────────────────────

    record ImportErrorResponse(int row, String email, String reason) {}

    record ImportResponse(int imported, int skipped, List<ImportErrorResponse> errors) {}

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResponse importCsv(@RequestParam("file") MultipartFile file) throws IOException, CsvException {
        List<EmployeeUseCase.CreateEmployeeCommand> commands = parseCsv(file);
        EmployeeUseCase.ImportResult result = employeeUseCase.importBatch(commands);
        return new ImportResponse(
                result.imported(),
                result.skipped(),
                result.errors().stream()
                        .map(e -> new ImportErrorResponse(e.row(), e.email(), e.reason()))
                        .toList());
    }

    private List<EmployeeUseCase.CreateEmployeeCommand> parseCsv(MultipartFile file) throws IOException, CsvException {
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csv = new CSVReader(reader)) {

            String[] headers = csv.readNext();
            if (headers == null) {
                throw new DomainException(new DomainError.Validation("CSV file is empty"));
            }

            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIdx.put(headers[i].trim().toLowerCase(), i);
            }

            int fiIdx = colIdx.getOrDefault("firstname", -1);
            int laIdx = colIdx.getOrDefault("lastname", -1);
            int maIdx = colIdx.getOrDefault("email", -1);

            if (fiIdx < 0 || laIdx < 0 || maIdx < 0) {
                throw new DomainException(new DomainError.Validation(
                        "CSV must contain columns: firstname, lastname, email"));
            }

            List<EmployeeUseCase.CreateEmployeeCommand> commands = new ArrayList<>();
            String[] line;
            while ((line = csv.readNext()) != null) {
                if (line.length <= Math.max(fiIdx, Math.max(laIdx, maIdx))) continue;
                commands.add(new EmployeeUseCase.CreateEmployeeCommand(
                        line[fiIdx].trim(),
                        line[laIdx].trim(),
                        line[maIdx].trim()));
            }
            return commands;
        }
    }

    private EmployeeResponse buildResponse(Employee employee) {
        List<AssignmentResponse> assignments = assignmentUseCase.findByEmployee(employee.getId())
                .stream()
                .map(a -> new AssignmentResponse(
                        a.getId(),
                        a.getTeamId(),
                        a.getTeamName(),
                        a.getEmployeeId(),
                        a.getEmployeeName(),
                        a.getAllocationPct(),
                        a.getRoleType(),
                        a.getRoleWeight(),
                        a.getStartDate() != null ? a.getStartDate().toString() : null,
                        a.getEndDate() != null ? a.getEndDate().toString() : null))
                .toList();
        return EmployeeResponse.from(employee, assignments);
    }
}
