package com.utmost.optimusplan.domain.model;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradeCostHistory {
    private UUID id;
    private UUID gradeId;
    private BigDecimal dailyCost;
    private LocalDate effectiveFrom;
    private LocalDateTime createdAt;
}
