package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.port.in.SnapshotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
public class SnapshotController {

    private final SnapshotUseCase snapshotUseCase;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SnapshotUseCase.SnapshotDTO> generateForTeam(
            @RequestParam UUID teamId,
            @RequestParam String month) {
        return snapshotUseCase.generateForTeam(teamId, month);
    }

    @PostMapping("/generate-all")
    @ResponseStatus(HttpStatus.CREATED)
    public void generateAll(@RequestParam String month) {
        snapshotUseCase.generateAll(month);
    }

    @GetMapping("/team/{teamId}")
    public List<SnapshotUseCase.SnapshotDTO> getByTeam(
            @PathVariable UUID teamId,
            @RequestParam String from,
            @RequestParam String to) {
        return snapshotUseCase.findByTeam(teamId, from, to);
    }
}
