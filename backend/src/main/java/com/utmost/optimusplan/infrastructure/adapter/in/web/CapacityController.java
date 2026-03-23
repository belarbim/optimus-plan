package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.port.in.CapacityUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/capacity")
@RequiredArgsConstructor
public class CapacityController {

    private final CapacityUseCase capacityUseCase;

    record SimulatedChangeRequest(
            @NotBlank String type,
            @NotNull UUID employeeId,
            String roleType,
            BigDecimal roleWeight,
            BigDecimal allocationPct) {}

    record SimulationRequest(
            @NotBlank String month,
            @NotNull List<SimulatedChangeRequest> changes) {}

    @GetMapping("/team/{teamId}")
    public CapacityUseCase.CapacityResult getCapacity(
            @PathVariable UUID teamId,
            @RequestParam String month) {
        return capacityUseCase.computeCapacity(new CapacityUseCase.ComputeCapacityQuery(teamId, month));
    }

    @GetMapping("/team/{teamId}/remaining")
    public CapacityUseCase.RemainingCapacityResult getRemaining(
            @PathVariable UUID teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return capacityUseCase.computeRemaining(new CapacityUseCase.RemainingCapacityQuery(teamId, date));
    }

    @GetMapping("/team/{teamId}/rollup")
    public CapacityUseCase.RollupResult getRollup(
            @PathVariable UUID teamId,
            @RequestParam String month) {
        return capacityUseCase.computeRollup(new CapacityUseCase.RollupQuery(teamId, month));
    }

    @PostMapping("/team/{teamId}/simulate")
    public CapacityUseCase.SimulationResult simulate(
            @PathVariable UUID teamId,
            @Valid @RequestBody SimulationRequest req) {
        List<CapacityUseCase.SimulatedChange> changes = req.changes().stream()
                .map(c -> new CapacityUseCase.SimulatedChange(
                        c.type(), c.employeeId(), c.roleType(), c.roleWeight(), c.allocationPct()))
                .toList();
        return capacityUseCase.simulate(new CapacityUseCase.SimulationQuery(teamId, req.month(), changes));
    }
}
