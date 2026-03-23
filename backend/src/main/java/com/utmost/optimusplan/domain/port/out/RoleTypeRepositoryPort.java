package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.RoleTypeConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleTypeRepositoryPort {

    RoleTypeConfig save(RoleTypeConfig roleTypeConfig);

    Optional<RoleTypeConfig> findById(UUID id);

    List<RoleTypeConfig> findAll();

    Optional<RoleTypeConfig> findByRoleType(String roleType);

    void deleteById(UUID id);

    boolean existsByRoleType(String roleType);

    /**
     * Returns true if the given role type config is referenced by at least one RoleHistory entry.
     */
    boolean isReferencedInHistory(UUID id);
}
