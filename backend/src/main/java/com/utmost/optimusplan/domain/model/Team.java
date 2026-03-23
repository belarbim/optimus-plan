package com.utmost.optimusplan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    private UUID id;
    private String name;
    private UUID parentId;

    @Builder.Default
    private List<Team> children = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
