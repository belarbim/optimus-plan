package com.utmost.optimusplan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicHoliday {

    private UUID id;
    private LocalDate date;
    private String name;
    private String locale;

    /** If true, the holiday repeats every year on the same month/day. */
    private boolean recurring;

    private LocalDateTime createdAt;
}
