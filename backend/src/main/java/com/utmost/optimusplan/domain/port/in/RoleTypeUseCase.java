package com.utmost.optimusplan.domain.port.in;

import com.utmost.optimusplan.domain.model.RoleTypeConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RoleTypeUseCase {

    record CreateRoleTypeCommand(String roleType, BigDecimal defaultWeight, String description) {}

    record UpdateRoleTypeCommand(
            UUID id,
            String roleType,
            BigDecimal defaultWeight,
            String description) {}

    List<RoleTypeConfig> findAll();

    RoleTypeConfig findById(UUID id);

    RoleTypeConfig create(CreateRoleTypeCommand cmd);

    RoleTypeConfig update(UpdateRoleTypeCommand cmd);

    void delete(UUID id);
}
