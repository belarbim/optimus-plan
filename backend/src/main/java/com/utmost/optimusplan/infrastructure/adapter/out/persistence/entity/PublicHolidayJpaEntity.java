package com.utmost.optimusplan.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "public_holiday")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicHolidayJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDate date;

    private String name;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean recurring;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
