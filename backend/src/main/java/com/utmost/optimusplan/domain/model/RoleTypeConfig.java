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
public class RoleTypeConfig {

    private UUID id;
    private String roleType;
    private BigDecimal defaultWeight;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
