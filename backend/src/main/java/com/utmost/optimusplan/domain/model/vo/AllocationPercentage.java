package com.utmost.optimusplan.domain.model.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AllocationPercentage(BigDecimal value) {

    public AllocationPercentage {
        if (value == null
                || value.compareTo(BigDecimal.ONE) < 0
                || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Allocation must be between 1 and 100");
        }
    }

    public static AllocationPercentage of(BigDecimal value) {
        return new AllocationPercentage(value);
    }

    public BigDecimal asFraction() {
        return value.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
    }
}
