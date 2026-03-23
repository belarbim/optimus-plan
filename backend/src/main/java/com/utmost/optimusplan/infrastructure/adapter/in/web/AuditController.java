package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.port.in.AuditUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditUseCase auditUseCase;

    record AuditLogResponse(
            UUID id,
            String entityType,
            UUID entityId,
            String action,
            Map<String, Object> changes,
            String actor,
            String timestamp) {

        static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(
                    log.getId(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getAction(),
                    log.getChanges(),
                    log.getActor(),
                    log.getTimestamp() != null ? log.getTimestamp().toString() : null);
        }
    }

    @GetMapping
    public Page<AuditLogResponse> query(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuditUseCase.AuditFilter filter = new AuditUseCase.AuditFilter(
                entityType, action, dateFrom, dateTo, page, size);
        return auditUseCase.query(filter).map(AuditLogResponse::from);
    }
}
