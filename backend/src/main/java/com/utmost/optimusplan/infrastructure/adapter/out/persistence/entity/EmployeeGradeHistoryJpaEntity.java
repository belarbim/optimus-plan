package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "employee_grade_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeGradeHistoryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grade_id", nullable = false)
    private GradeJpaEntity grade;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
