package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.AuditLog;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface AuditUseCase {

    record AuditFilter(
            String entityType,
            String action,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            int page,
            int size) {}

    void log(String entityType, UUID entityId, String action, Map<String, Object> changes);

    Page<AuditLog> query(AuditFilter filter);
}
