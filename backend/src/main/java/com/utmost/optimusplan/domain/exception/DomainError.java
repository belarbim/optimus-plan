package com.utmost.optimusplan.domain.exception;

import java.util.List;
import java.util.UUID;

public sealed interface DomainError
        permits DomainError.NotFound, DomainError.Conflict,
                DomainError.BusinessRule, DomainError.Validation {

    record NotFound(String entityType, String id) implements DomainError {
        public NotFound(String entityType, UUID id) {
            this(entityType, id.toString());
        }

        public String message() {
            return entityType + " not found: " + id;
        }
    }

    record Conflict(String message) implements DomainError {}

    record BusinessRule(String message) implements DomainError {}

    record Validation(List<String> violations) implements DomainError {
        public Validation(String violation) {
            this(List.of(violation));
        }
    }
}
