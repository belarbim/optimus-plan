package com.utmost.optimusplan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    private UUID id;
    private String entityType;
    private UUID entityId;
    private String action;
    private Map<String, Object> changes;
    private String actor;
    private LocalDateTime timestamp;
}
