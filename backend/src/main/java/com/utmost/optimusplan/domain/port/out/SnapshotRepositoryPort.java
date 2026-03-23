package com.utmost.optimusplan.domain.port.out;

import com.utmost.optimusplan.domain.model.CapacitySnapshot;

import java.util.List;
import java.util.UUID;

public interface SnapshotRepositoryPort {

    List<CapacitySnapshot> saveAll(List<CapacitySnapshot> snapshots);

    /**
     * Returns snapshots for a team whose snapshotMonth is within the [from, to] range
     * (both inclusive, in yyyy-MM format).
     */
    List<CapacitySnapshot> findByTeamIdAndMonthRange(UUID teamId, String from, String to);

    void deleteByTeamIdAndMonth(UUID teamId, String month);
}
