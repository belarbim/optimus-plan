package com.utmost.optimusplan.application.team;

import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.AuditLog;
import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.port.in.TeamUseCase.CreateTeamCommand;
import com.utmost.optimusplan.domain.port.in.TeamUseCase.UpdateTeamCommand;
import com.utmost.optimusplan.domain.port.out.AssignmentRepositoryPort;
import com.utmost.optimusplan.domain.port.out.AuditRepositoryPort;
import com.utmost.optimusplan.domain.port.out.TeamRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TeamApplicationServiceTest {

    @Mock TeamRepositoryPort teamRepo;
    @Mock AssignmentRepositoryPort assignmentRepo;
    @Mock AuditRepositoryPort auditRepo;

    TeamApplicationService service;

    @BeforeEach
    void setUp() {
        service = new TeamApplicationService(teamRepo, assignmentRepo, auditRepo);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_rootTeam_persistsAndReturns() {
        UUID id = UUID.randomUUID();
        given(teamRepo.existsByNameAndParentIsNull("Alpha")).willReturn(false);
        given(teamRepo.save(any())).willReturn(team(id, "Alpha", null));

        Team result = service.create(new CreateTeamCommand("Alpha", null));

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Alpha");
        assertThat(result.getParentId()).isNull();
    }

    @Test
    void create_subTeam_loadsParentAndPersists() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        given(teamRepo.existsByNameAndParentId("Child", parentId)).willReturn(false);
        given(teamRepo.findById(parentId)).willReturn(Optional.of(team(parentId, "Parent", null)));
        given(teamRepo.save(any())).willReturn(team(childId, "Child", parentId));

        Team result = service.create(new CreateTeamCommand("Child", parentId));

        assertThat(result.getParentId()).isEqualTo(parentId);
    }

    @Test
    void create_duplicateRootName_throwsConflict() {
        given(teamRepo.existsByNameAndParentIsNull("Alpha")).willReturn(true);

        assertThatThrownBy(() -> service.create(new CreateTeamCommand("Alpha", null)))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.Conflict.class);

        then(teamRepo).should(never()).save(any());
    }

    @Test
    void create_duplicateNameUnderParent_throwsConflict() {
        UUID parentId = UUID.randomUUID();
        given(teamRepo.existsByNameAndParentId("Child", parentId)).willReturn(true);

        assertThatThrownBy(() -> service.create(new CreateTeamCommand("Child", parentId)))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.Conflict.class);
    }

    @Test
    void create_parentNotFound_throwsNotFound() {
        UUID parentId = UUID.randomUUID();
        given(teamRepo.existsByNameAndParentId("Child", parentId)).willReturn(false);
        given(teamRepo.findById(parentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateTeamCommand("Child", parentId)))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.NotFound.class)
                .extracting(e -> ((DomainError.NotFound) e).entityType())
                .isEqualTo("Team");
    }

    @Test
    void create_writesAuditLog() {
        UUID id = UUID.randomUUID();
        given(teamRepo.existsByNameAndParentIsNull("Alpha")).willReturn(false);
        given(teamRepo.save(any())).willReturn(team(id, "Alpha", null));
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        service.create(new CreateTeamCommand("Alpha", null));

        then(auditRepo).should().save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo("CREATE");
        assertThat(log.getEntityType()).isEqualTo("Team");
        assertThat(log.getEntityId()).isEqualTo(id);
        assertThat(log.getActor()).isEqualTo("manager");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_newName_checksUniquenessAndPersists() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.of(team(id, "OldName", null)));
        given(teamRepo.existsByNameAndParentIsNull("NewName")).willReturn(false);
        given(teamRepo.save(any())).willReturn(team(id, "NewName", null));

        Team result = service.update(new UpdateTeamCommand(id, "NewName"));

        assertThat(result.getName()).isEqualTo("NewName");
    }

    @Test
    void update_sameName_skipsUniquenessCheck() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.of(team(id, "Same", null)));
        given(teamRepo.save(any())).willReturn(team(id, "Same", null));

        service.update(new UpdateTeamCommand(id, "Same"));

        then(teamRepo).should(never()).existsByNameAndParentIsNull(any());
        then(teamRepo).should(never()).existsByNameAndParentId(any(), any());
    }

    @Test
    void update_newNameAlreadyTaken_throwsConflict() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.of(team(id, "Old", null)));
        given(teamRepo.existsByNameAndParentIsNull("Taken")).willReturn(true);

        assertThatThrownBy(() -> service.update(new UpdateTeamCommand(id, "Taken")))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.Conflict.class);
    }

    @Test
    void update_notFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new UpdateTeamCommand(id, "X")))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.NotFound.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_leafTeamWithNoAssignments_succeeds() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.of(team(id, "Leaf", null)));
        given(teamRepo.hasChildren(id)).willReturn(false);
        given(assignmentRepo.hasActiveAssignmentsByTeamId(id)).willReturn(false);

        service.delete(id);

        then(teamRepo).should().deleteById(id);
    }

    @Test
    void delete_teamWithChildren_throwsBusinessRule() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.of(team(id, "Parent", null)));
        given(teamRepo.hasChildren(id)).willReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.BusinessRule.class)
                .extracting(e -> ((DomainError.BusinessRule) e).message())
                .asString().contains("sub-teams");

        then(teamRepo).should(never()).deleteById(any());
    }

    @Test
    void delete_teamWithActiveAssignments_throwsBusinessRule() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.of(team(id, "Team", null)));
        given(teamRepo.hasChildren(id)).willReturn(false);
        given(assignmentRepo.hasActiveAssignmentsByTeamId(id)).willReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.BusinessRule.class)
                .extracting(e -> ((DomainError.BusinessRule) e).message())
                .asString().contains("assignments");
    }

    @Test
    void delete_notFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.NotFound.class);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_tree_delegatesToFindRoots() {
        List<Team> roots = List.of(team(UUID.randomUUID(), "R1", null));
        given(teamRepo.findRoots()).willReturn(roots);

        assertThat(service.findAll(true)).isSameAs(roots);
        then(teamRepo).should(never()).findAll();
    }

    @Test
    void findAll_flat_delegatesToFindAll() {
        List<Team> all = List.of(
                team(UUID.randomUUID(), "A", null),
                team(UUID.randomUUID(), "B", null));
        given(teamRepo.findAll()).willReturn(all);

        assertThat(service.findAll(false)).hasSize(2);
        then(teamRepo).should(never()).findRoots();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existing_returnsTeam() {
        UUID id = UUID.randomUUID();
        Team t = team(id, "T", null);
        given(teamRepo.findById(id)).willReturn(Optional.of(t));

        assertThat(service.findById(id)).isSameAs(t);
    }

    @Test
    void findById_missing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        given(teamRepo.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isInstanceOf(DomainError.NotFound.class);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static Team team(UUID id, String name, UUID parentId) {
        return Team.builder()
                .id(id).name(name).parentId(parentId)
                .children(List.of())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
