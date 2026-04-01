package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepositoryPort {

    Team save(Team team);

    Optional<Team> findById(UUID id);

    List<Team> findAll();

    List<Team> findRoots();

    List<Team> findByParentId(UUID parentId);

    void deleteById(UUID id);

    boolean existsByNameAndParentId(String name, UUID parentId);

    boolean existsByNameAndParentIsNull(String name);

    boolean hasChildren(UUID id);

    boolean existsById(UUID id);

    Optional<Team> findByNameIgnoreCase(String name);
}
