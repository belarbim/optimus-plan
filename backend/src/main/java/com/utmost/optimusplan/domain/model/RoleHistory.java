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
public class RoleHistory {

    private UUID id;
    private UUID assignmentId;
    private String roleType;
    private BigDecimal roleWeight;
    private LocalDate effectiveFrom;

    /** Nullable – null means this is the currently active role segment. */
    private LocalDate effectiveTo;

    private LocalDateTime createdAt;
}
