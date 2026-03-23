package com.utmost.optimusplan.domain.exception;

public class DomainException extends RuntimeException {

    private final DomainError error;

    public DomainException(DomainError error) {
        super(errorMessage(error));
        this.error = error;
    }

    public DomainError error() {
        return error;
    }

    private static String errorMessage(DomainError error) {
        return switch (error) {
            case DomainError.NotFound e     -> e.message();
            case DomainError.Conflict e     -> e.message();
            case DomainError.BusinessRule e -> e.message();
            case DomainError.Validation e   -> String.join(", ", e.violations());
        };
    }
}
