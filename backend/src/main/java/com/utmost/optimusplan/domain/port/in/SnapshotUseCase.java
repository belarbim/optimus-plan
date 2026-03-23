package com.utmost.optimusplan.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SnapshotUseCase {

    record SnapshotDTO(
            UUID id,
            UUID teamId,
            String snapshotMonth,
            String categoryName,
            BigDecimal capacityManDays,
            LocalDateTime createdAt) {}

    /**
     * Computes capacity for the team in the given month, replaces any existing snapshots
     * for that team/month, and returns the new snapshot DTOs.
     */
    List<SnapshotDTO> generateForTeam(UUID teamId, String month);

    /**
     * Generates snapshots for every team for the given month. Exceptions per team are
     * caught and logged so that one failure does not abort the others.
     */
    void generateAll(String month);

    List<SnapshotDTO> findByTeam(UUID teamId, String from, String to);
}
