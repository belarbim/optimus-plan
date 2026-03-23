package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.utmost.optimusplan.domain.model.RoleTypeConfig;
import com.utmost.optimusplan.domain.port.in.RoleTypeUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/role-types")
@RequiredArgsConstructor
public class RoleTypeController {

    private final RoleTypeUseCase roleTypeUseCase;

    record RoleTypeRequest(
            @NotBlank String roleType,
            @NotNull BigDecimal defaultWeight,
            String description) {}

    record RoleTypeResponse(
            UUID id,
            String roleType,
            BigDecimal defaultWeight,
            String description) {

        static RoleTypeResponse from(RoleTypeConfig c) {
            return new RoleTypeResponse(c.getId(), c.getRoleType(), c.getDefaultWeight(), c.getDescription());
        }
    }

    @GetMapping
    public List<RoleTypeResponse> getAll() {
        return roleTypeUseCase.findAll().stream().map(RoleTypeResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RoleTypeResponse getById(@PathVariable UUID id) {
        return RoleTypeResponse.from(roleTypeUseCase.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleTypeResponse create(@Valid @RequestBody RoleTypeRequest req) {
        return RoleTypeResponse.from(
                roleTypeUseCase.create(new RoleTypeUseCase.CreateRoleTypeCommand(
                        req.roleType(), req.defaultWeight(), req.description())));
    }

    @PutMapping("/{id}")
    public RoleTypeResponse update(@PathVariable UUID id, @Valid @RequestBody RoleTypeRequest req) {
        return RoleTypeResponse.from(
                roleTypeUseCase.update(new RoleTypeUseCase.UpdateRoleTypeCommand(
                        id, req.roleType(), req.defaultWeight(), req.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        roleTypeUseCase.delete(id);
    }
}
