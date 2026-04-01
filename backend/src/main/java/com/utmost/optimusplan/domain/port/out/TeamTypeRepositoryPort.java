package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.TeamType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamTypeRepositoryPort {

    TeamType save(TeamType teamType);

    Optional<TeamType> findById(UUID id);

    List<TeamType> findAll();

    void deleteById(UUID id);

    boolean existsByName(String name);

    boolean existsById(UUID id);
}
