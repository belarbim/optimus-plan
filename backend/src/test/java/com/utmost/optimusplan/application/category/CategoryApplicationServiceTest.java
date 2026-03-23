package com.utmost.optimusplan.application.category;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.CategoryAllocation;
import com.utmost.optimusplan.domain.port.in.CategoryUseCase;
import com.utmost.optimusplan.domain.port.in.CategoryUseCase.CategoryEntry;
import com.utmost.optimusplan.domain.port.in.CategoryUseCase.SetCategoriesCommand;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.CategoryRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.never;

@ExtendWith(MockitoExtension.class)
class CategoryApplicationServiceTest {

    @Mock CategoryRepositoryPort categoryRepo;
    @Mock TeamRepositoryPort teamRepo;
    @Mock AuditRepositoryPort auditRepo;

    CategoryApplicationService service;

    private final UUID teamId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CategoryApplicationService(categoryRepo, teamRepo, auditRepo);
    }

    // ── findByTeam ────────────────────────────────────────────────────────────

    @Test
    void findByTeam_returnsSavedAllocations() {
        List<CategoryAllocation> allocations = List.of(allocation("Project", 60), allocation("Incident", 20));
        given(categoryRepo.findByTeamId(teamId)).willReturn(allocations);

        assertThat(service.findByTeam(teamId)).isSameAs(allocations);
    }

    // ── setCategories ─────────────────────────────────────────────────────────

    @Test
    void setCategories_validInput_deletesAndSavesAll() {
        given(teamRepo.existsById(teamId)).willReturn(true);
        given(categoryRepo.save(any())).willAnswer(i -> i.getArgument(0));

        List<CategoryAllocation> result = service.setCategories(validCommand(teamId));

        then(categoryRepo).should().deleteByTeamId(teamId);
        assertThat(result).hasSize(4);
    }

    @Test
    void setCategories_teamNotFound_throwsNotFound() {
        given(teamRepo.existsById(teamId)).willReturn(false);

        assertThatThrownBy(() -> service.setCategories(validCommand(teamId)))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.NotFound.class)
                .extracting(e -> ((DomainError.NotFound) e).entityType())
                .isEqualTo("Team");

        then(categoryRepo).should(never()).deleteByTeamId(any());
    }

    @Test
    void setCategories_incidentAbove100_throwsBusinessRule() {
        given(teamRepo.existsById(teamId)).willReturn(true);

        SetCategoriesCommand cmd = new SetCategoriesCommand(teamId, List.of(
                new CategoryEntry("Incident", new BigDecimal("110")),
                new CategoryEntry("Project", new BigDecimal("50")),
                new CategoryEntry("Continuous Improvement", new BigDecimal("30")),
                new CategoryEntry("IT for IT", new BigDecimal("20"))
        ));

        assertThatThrownBy(() -> service.setCategories(cmd))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.BusinessRule.class)
                .extracting(e -> ((DomainError.BusinessRule) e).message())
                .asString().contains("Incident allocation");
    }

    @Test
    void setCategories_incidentNegative_throwsBusinessRule() {
        given(teamRepo.existsById(teamId)).willReturn(true);

        SetCategoriesCommand cmd = new SetCategoriesCommand(teamId, List.of(
                new CategoryEntry("Incident", new BigDecimal("-5")),
                new CategoryEntry("Project", new BigDecimal("50")),
                new CategoryEntry("Continuous Improvement", new BigDecimal("30")),
                new CategoryEntry("IT for IT", new BigDecimal("20"))
        ));

        assertThatThrownBy(() -> service.setCategories(cmd))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.BusinessRule.class);
    }

    @Test
    void setCategories_plannedSumNot100_throwsBusinessRule() {
        given(teamRepo.existsById(teamId)).willReturn(true);

        SetCategoriesCommand cmd = new SetCategoriesCommand(teamId, List.of(
                new CategoryEntry("Incident", new BigDecimal("20")),
                new CategoryEntry("Project", new BigDecimal("40")),
                new CategoryEntry("Continuous Improvement", new BigDecimal("30")),
                new CategoryEntry("IT for IT", new BigDecimal("20")) // 40+30+20 = 90, not 100
        ));

        assertThatThrownBy(() -> service.setCategories(cmd))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.BusinessRule.class)
                .extracting(e -> ((DomainError.BusinessRule) e).message())
                .asString().contains("sum to 100");
    }

    @Test
    void setCategories_plannedSumExceeds100_throwsBusinessRule() {
        given(teamRepo.existsById(teamId)).willReturn(true);

        SetCategoriesCommand cmd = new SetCategoriesCommand(teamId, List.of(
                new CategoryEntry("Incident", new BigDecimal("20")),
                new CategoryEntry("Project", new BigDecimal("50")),
                new CategoryEntry("Continuous Improvement", new BigDecimal("40")),
                new CategoryEntry("IT for IT", new BigDecimal("20")) // 110, not 100
        ));

        assertThatThrownBy(() -> service.setCategories(cmd))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.BusinessRule.class);
    }

    @Test
    void setCategories_incidentAtBoundary0_isValid() {
        given(teamRepo.existsById(teamId)).willReturn(true);
        given(categoryRepo.save(any())).willAnswer(i -> i.getArgument(0));

        SetCategoriesCommand cmd = new SetCategoriesCommand(teamId, List.of(
                new CategoryEntry("Incident", BigDecimal.ZERO),
                new CategoryEntry("Project", new BigDecimal("50")),
                new CategoryEntry("Continuous Improvement", new BigDecimal("30")),
                new CategoryEntry("IT for IT", new BigDecimal("20"))
        ));

        assertThat(service.setCategories(cmd)).hasSize(4);
    }

    @Test
    void setCategories_incidentAt100_isValid() {
        given(teamRepo.existsById(teamId)).willReturn(true);
        given(categoryRepo.save(any())).willAnswer(i -> i.getArgument(0));

        SetCategoriesCommand cmd = new SetCategoriesCommand(teamId, List.of(
                new CategoryEntry("Incident", new BigDecimal("100")),
                new CategoryEntry("Project", new BigDecimal("50")),
                new CategoryEntry("Continuous Improvement", new BigDecimal("30")),
                new CategoryEntry("IT for IT", new BigDecimal("20"))
        ));

        assertThat(service.setCategories(cmd)).hasSize(4);
    }

    @Test
    void setCategories_caseInsensitiveIncidentName_validates() {
        given(teamRepo.existsById(teamId)).willReturn(true);

        SetCategoriesCommand cmd = new SetCategoriesCommand(teamId, List.of(
                new CategoryEntry("INCIDENT", new BigDecimal("150")), // case insensitive, invalid
                new CategoryEntry("Project", new BigDecimal("50")),
                new CategoryEntry("Continuous Improvement", new BigDecimal("30")),
                new CategoryEntry("IT for IT", new BigDecimal("20"))
        ));

        assertThatThrownBy(() -> service.setCategories(cmd))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.BusinessRule.class);
    }

    @Test
    void setCategories_recordsAuditLog() {
        given(teamRepo.existsById(teamId)).willReturn(true);
        given(categoryRepo.save(any())).willAnswer(i -> i.getArgument(0));

        service.setCategories(validCommand(teamId));

        then(auditRepo).should().save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static SetCategoriesCommand validCommand(UUID teamId) {
        return new SetCategoriesCommand(teamId, List.of(
                new CategoryEntry("Incident", new BigDecimal("20")),
                new CategoryEntry("Project", new BigDecimal("50")),
                new CategoryEntry("Continuous Improvement", new BigDecimal("30")),
                new CategoryEntry("IT for IT", new BigDecimal("20"))
        ));
    }

    private static CategoryAllocation allocation(String name, int pct) {
        return CategoryAllocation.builder()
                .id(UUID.randomUUID())
                .categoryName(name)
                .allocationPct(BigDecimal.valueOf(pct))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
