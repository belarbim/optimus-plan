package com.utmost.optimusplan.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamType {

    private UUID   id;
    private String name;

    @Builder.Default
    private List<TeamTypeCategory> categories = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
