package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.CapacityAlert;

import java.util.Optional;
import java.util.UUID;

public interface AlertRepositoryPort {

    CapacityAlert save(CapacityAlert alert);

    Optional<CapacityAlert> findById(UUID id);

    Optional<CapacityAlert> findByTeamId(UUID teamId);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
