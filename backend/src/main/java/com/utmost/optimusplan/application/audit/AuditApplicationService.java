package com.utmost.optimusplan.application.audit;

import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.port.in.AuditUseCase;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuditApplicationService implements AuditUseCase {

    private final AuditRepositoryPort auditRepo;

    public AuditApplicationService(AuditRepositoryPort auditRepo) {
        this.auditRepo = auditRepo;
    }

    // -------------------------------------------------------------------------
    // AuditUseCase
    // -------------------------------------------------------------------------

    @Override
    public void log(String entityType, UUID entityId, String action,
                    Map<String, Object> changes) {
        auditRepo.save(AuditLog.builder()
                .id(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .changes(changes)
                .actor("manager")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> query(AuditFilter filter) {
        Pageable pageable = PageRequest.of(
                Math.max(0, filter.page()),
                filter.size() > 0 ? filter.size() : 20);

        return auditRepo.findWithFilters(
                filter.entityType(),
                filter.action(),
                filter.dateFrom(),
                filter.dateTo(),
                pageable);
    }
}
