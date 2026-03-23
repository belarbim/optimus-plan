package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.Team;

import java.util.List;
import java.util.UUID;

public interface TeamUseCase {

    record CreateTeamCommand(String name, UUID parentId) {}

    record UpdateTeamCommand(UUID id, String name) {}

    Team create(CreateTeamCommand cmd);

    Team update(UpdateTeamCommand cmd);

    void delete(UUID id);

    Team findById(UUID id);

    List<Team> findAll(boolean tree);
}
