package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.CapacityAlert;

import java.math.BigDecimal;
import java.util.UUID;

public interface AlertUseCase {

    record CreateAlertCommand(UUID teamId, BigDecimal thresholdManDays, boolean enabled) {}

    CapacityAlert createOrUpdate(CreateAlertCommand cmd);

    CapacityAlert findByTeam(UUID teamId);

    void delete(UUID id);
}
