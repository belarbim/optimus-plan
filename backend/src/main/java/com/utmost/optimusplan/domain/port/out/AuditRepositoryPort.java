package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditRepositoryPort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    Page<AuditLog> findWithFilters(String entityType,
                                   String action,
                                   LocalDateTime from,
                                   LocalDateTime to,
                                   Pageable pageable);
}
