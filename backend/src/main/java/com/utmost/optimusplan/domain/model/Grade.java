package com.utmost.optimusplan.domain.model;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Grade {
    private UUID id;
    private String name;
    private BigDecimal dailyCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
