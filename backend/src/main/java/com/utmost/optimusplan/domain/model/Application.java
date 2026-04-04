package com.utmost.optimusplan.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Application {
    private UUID id;
    private String name;
    private String description;
    private UUID teamId;
    private String teamName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
