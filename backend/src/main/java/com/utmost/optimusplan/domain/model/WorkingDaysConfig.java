package com.utmost.optimusplan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkingDaysConfig {

    private UUID id;

    /** Month in yyyy-MM format. */
    private String month;

    private BigDecimal avgDaysWorked;
    private LocalDateTime importedAt;
}
