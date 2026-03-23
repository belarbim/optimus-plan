package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.CapacityAlert;
import com.utmost.optimusplan.domain.port.in.AlertUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertUseCase alertUseCase;

    record AlertRequest(
            @NotNull UUID teamId,
            @NotNull BigDecimal thresholdManDays,
            boolean enabled) {}

    record AlertResponse(
            UUID id,
            UUID teamId,
            BigDecimal thresholdManDays,
            boolean enabled) {

        static AlertResponse from(CapacityAlert a) {
            return new AlertResponse(a.getId(), a.getTeamId(), a.getThresholdManDays(), a.isEnabled());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertResponse createOrUpdate(@Valid @RequestBody AlertRequest req) {
        return AlertResponse.from(
                alertUseCase.createOrUpdate(new AlertUseCase.CreateAlertCommand(
                        req.teamId(), req.thresholdManDays(), req.enabled())));
    }

    @GetMapping("/team/{teamId}")
    public AlertResponse getByTeam(@PathVariable UUID teamId) {
        return AlertResponse.from(alertUseCase.findByTeam(teamId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        alertUseCase.delete(id);
    }
}
