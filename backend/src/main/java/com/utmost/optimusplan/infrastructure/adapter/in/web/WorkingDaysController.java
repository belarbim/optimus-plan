package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.WorkingDaysConfig;
import com.utmost.optimusplan.domain.port.in.WorkingDaysUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/working-days")
@RequiredArgsConstructor
public class WorkingDaysController {

    private final WorkingDaysUseCase workingDaysUseCase;

    record WorkingDaysResponse(UUID id, String month, BigDecimal avgDaysWorked) {

        static WorkingDaysResponse from(WorkingDaysConfig c) {
            return new WorkingDaysResponse(c.getId(), c.getMonth(), c.getAvgDaysWorked());
        }
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public List<WorkingDaysResponse> importCsv(@RequestParam("file") MultipartFile file) {
        return workingDaysUseCase.importCsv(file).stream()
                .map(WorkingDaysResponse::from)
                .toList();
    }

    @GetMapping
    public List<WorkingDaysResponse> getAll() {
        return workingDaysUseCase.findAll().stream()
                .map(WorkingDaysResponse::from)
                .toList();
    }
}
