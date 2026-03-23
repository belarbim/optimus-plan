package com.utmost.optimusplan.domain.model.vo;

import java.math.BigDecimal;

public record RoleWeight(BigDecimal value) {

    public RoleWeight {
        if (value == null
                || value.compareTo(BigDecimal.ZERO) < 0
                || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Role weight must be between 0 and 1");
        }
    }

    public static RoleWeight of(BigDecimal value) {
        return new RoleWeight(value);
    }
}
