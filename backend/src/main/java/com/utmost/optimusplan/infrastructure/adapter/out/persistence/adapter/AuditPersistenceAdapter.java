package com.utmost.optimusplan.infrastructure.adapter.out.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity.AuditLogJpaEntity;
import com.utmost.optimusplan.infrastructure.adapter.out.persistence.jpa.AuditJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditPersistenceAdapter implements AuditRepositoryPort {

    private final AuditJpaRepository repo;
    private final ObjectMapper objectMapper;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public AuditLog save(AuditLog auditLog) {
        return toDomain(repo.save(toEntity(auditLog)));
    }

    @Override
    public List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId) {
        return repo.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<AuditLog> findWithFilters(String entityType, String action,
                                          LocalDateTime from, LocalDateTime to,
                                          Pageable pageable) {
        Specification<AuditLogJpaEntity> spec = Specification.where(null);
        if (entityType != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (action != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("action"), action));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }
        return repo.findAll(spec, pageable).map(this::toDomain);
    }

    private AuditLog toDomain(AuditLogJpaEntity e) {
        Map<String, Object> changes = deserializeChanges(e.getChangesJson());
        return AuditLog.builder()
                .id(e.getId())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .action(e.getAction())
                .changes(changes)
                .actor(e.getActor())
                .timestamp(e.getTimestamp())
                .build();
    }

    private AuditLogJpaEntity toEntity(AuditLog auditLog) {
        String changesJson = serializeChanges(auditLog.getChanges());
        return AuditLogJpaEntity.builder()
                .id(auditLog.getId())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .changesJson(changesJson)
                .actor(auditLog.getActor())
                .timestamp(auditLog.getTimestamp())
                .build();
    }

    private String serializeChanges(Map<String, Object> changes) {
        if (changes == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit changes to JSON", e);
            return "{}";
        }
    }

    private Map<String, Object> deserializeChanges(String changesJson) {
        if (changesJson == null || changesJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(changesJson, MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize audit changes from JSON: {}", changesJson, e);
            return Collections.emptyMap();
        }
    }
}
