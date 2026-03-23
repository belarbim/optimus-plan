package com.utmost.optimusplan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAssignment {

    private UUID id;
    private UUID teamId;
    private String teamName;
    private UUID employeeId;
    private String employeeName;
    private BigDecimal allocationPct;
    private String roleType;
    private BigDecimal roleWeight;
    private LocalDate startDate;

    /** Nullable – null means the assignment is open-ended. */
    private LocalDate endDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** True if this assignment has no end date or the end date is in the future. */
    public boolean isActive() {
        return endDate == null || endDate.isAfter(LocalDate.now());
    }

    /** True if the assignment covers the given calendar date. */
    public boolean isActiveOn(LocalDate date) {
        return !startDate.isAfter(date) && (endDate == null || !endDate.isBefore(date));
    }
}
