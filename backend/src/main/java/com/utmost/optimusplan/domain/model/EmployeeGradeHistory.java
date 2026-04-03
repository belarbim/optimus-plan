package com.utmost.optimusplan.domain.model;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeGradeHistory {
    private UUID id;
    private UUID employeeId;
    private UUID gradeId;
    private String gradeName;
    private BigDecimal dailyCost;
    private LocalDate effectiveFrom;
    private LocalDateTime createdAt;
}
