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
public class CategoryAllocation {

    private UUID id;
    private UUID teamId;
    private String categoryName;
    private BigDecimal allocationPct;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
