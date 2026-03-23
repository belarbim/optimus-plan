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
public class CapacitySnapshot {

    private UUID id;
    private UUID teamId;

    /** Month in yyyy-MM format. */
    private String snapshotMonth;

    private String categoryName;
    private BigDecimal capacityManDays;
    private LocalDateTime createdAt;
}
