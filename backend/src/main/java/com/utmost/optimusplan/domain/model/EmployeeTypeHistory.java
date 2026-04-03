package com.utmost.optimusplan.domain.model;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeTypeHistory {
    private UUID id;
    private UUID employeeId;
    private String type;
    private LocalDate effectiveFrom;
    private LocalDateTime createdAt;
}
