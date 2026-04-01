package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.RoleHistory;
import com.utmost.optimusplan.domain.model.TeamAssignment;
import com.utmost.optimusplan.domain.port.in.AssignmentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentUseCase assignmentUseCase;

    record CreateAssignmentRequest(
            @NotNull UUID teamId,
            @NotNull UUID employeeId,
            @NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal allocationPct,
            @NotBlank String roleType,
            @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal roleWeight,
            @NotNull LocalDate startDate,
            LocalDate endDate) {}

    record UpdateAssignmentRequest(
            @NotNull UUID teamId,
            @NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal allocationPct,
            @NotBlank String roleType,
            @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal roleWeight,
            @NotNull LocalDate startDate,
            LocalDate endDate) {}

    record UpdateAllocationRequest(
            @NotNull @DecimalMin("1") @DecimalMax("100") BigDecimal allocationPct) {}

    record ChangeRoleRequest(
            @NotBlank String roleType,
            @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal roleWeight,
            @NotNull LocalDate effectiveFrom) {}

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
            String endDate,
            String createdAt,
            String updatedAt) {

        static AssignmentResponse from(TeamAssignment a) {
            return new AssignmentResponse(
                    a.getId(),
                    a.getTeamId(),
                    a.getTeamName(),
                    a.getEmployeeId(),
                    a.getEmployeeName(),
                    a.getAllocationPct(),
                    a.getRoleType(),
                    a.getRoleWeight(),
                    a.getStartDate() != null ? a.getStartDate().toString() : null,
                    a.getEndDate() != null ? a.getEndDate().toString() : null,
                    a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                    a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null);
        }
    }

    record RoleHistoryResponse(
            UUID id,
            UUID assignmentId,
            String roleType,
            BigDecimal roleWeight,
            String effectiveFrom,
            String effectiveTo,
            String createdAt) {

        static RoleHistoryResponse from(RoleHistory rh) {
            return new RoleHistoryResponse(
                    rh.getId(),
                    rh.getAssignmentId(),
                    rh.getRoleType(),
                    rh.getRoleWeight(),
                    rh.getEffectiveFrom() != null ? rh.getEffectiveFrom().toString() : null,
                    rh.getEffectiveTo() != null ? rh.getEffectiveTo().toString() : null,
                    rh.getCreatedAt() != null ? rh.getCreatedAt().toString() : null);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentResponse create(@Valid @RequestBody CreateAssignmentRequest req) {
        TeamAssignment assignment = assignmentUseCase.assign(new AssignmentUseCase.CreateAssignmentCommand(
                req.teamId(), req.employeeId(), req.allocationPct(),
                req.roleType(), req.roleWeight(), req.startDate(), req.endDate()));
        return AssignmentResponse.from(assignment);
    }

    @PutMapping("/{id}")
    public AssignmentResponse updateAssignment(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateAssignmentRequest req) {
        return AssignmentResponse.from(
                assignmentUseCase.updateAssignment(new AssignmentUseCase.UpdateAssignmentCommand(
                        id, req.teamId(), req.allocationPct(), req.roleType(), req.roleWeight(),
                        req.startDate(), req.endDate())));
    }

    @PutMapping("/{id}/end")
    public AssignmentResponse endAssignment(@PathVariable UUID id, @RequestParam LocalDate endDate) {
        return AssignmentResponse.from(
                assignmentUseCase.endAssignment(new AssignmentUseCase.EndAssignmentCommand(id, endDate)));
    }

    @PutMapping("/{id}/allocation")
    public AssignmentResponse updateAllocation(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateAllocationRequest req) {
        return AssignmentResponse.from(
                assignmentUseCase.updateAllocation(
                        new AssignmentUseCase.UpdateAllocationCommand(id, req.allocationPct())));
    }

    @PutMapping("/{id}/role")
    public RoleHistoryResponse changeRole(@PathVariable UUID id,
                                          @Valid @RequestBody ChangeRoleRequest req) {
        return RoleHistoryResponse.from(
                assignmentUseCase.changeRole(new AssignmentUseCase.ChangeRoleCommand(
                        id, req.roleType(), req.roleWeight(), req.effectiveFrom())));
    }

    @GetMapping("/team/{teamId}")
    public List<AssignmentResponse> getByTeam(@PathVariable UUID teamId) {
        return assignmentUseCase.findByTeam(teamId).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @GetMapping("/employee/{employeeId}")
    public List<AssignmentResponse> getByEmployee(@PathVariable UUID employeeId) {
        return assignmentUseCase.findByEmployee(employeeId).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @GetMapping("/{id}/role-history")
    public List<RoleHistoryResponse> getRoleHistory(@PathVariable UUID id) {
        return assignmentUseCase.getRoleHistory(id).stream()
                .map(RoleHistoryResponse::from)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable UUID id) {
        assignmentUseCase.deleteAssignment(id);
    }
}
