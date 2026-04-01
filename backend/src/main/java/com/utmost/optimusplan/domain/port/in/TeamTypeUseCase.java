package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.TeamType;

import java.util.List;
import java.util.UUID;

public interface TeamTypeUseCase {

    record CategoryDefinition(
            String name,
            boolean isPartOfTotalCapacity,
            boolean isPartOfRemainingCapacity) {}

    record CreateTeamTypeCommand(String name, List<CategoryDefinition> categories) {}

    record UpdateTeamTypeCommand(UUID id, String name, List<CategoryDefinition> categories) {}

    TeamType create(CreateTeamTypeCommand cmd);

    TeamType update(UpdateTeamTypeCommand cmd);

    void delete(UUID id);

    TeamType findById(UUID id);

    List<TeamType> findAll();
}
