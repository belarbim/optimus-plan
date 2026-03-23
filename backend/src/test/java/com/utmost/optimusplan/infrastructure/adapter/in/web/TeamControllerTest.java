package com.utmost.optimusplan.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utmost.optimusplan.domain.exception.DomainError;
import com.utmost.optimusplan.domain.exception.DomainException;
import com.utmost.optimusplan.domain.model.Team;
import com.utmost.optimusplan.domain.port.in.TeamUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
class TeamControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean TeamUseCase teamUseCase;

    // ── POST /api/teams ───────────────────────────────────────────────────────

    @Test
    void createTeam_validRequest_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        given(teamUseCase.create(any())).willReturn(team(id, "Alpha", null));

        mvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Alpha"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("Alpha")));
    }

    @Test
    void createTeam_withParent_returns201WithParentId() throws Exception {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        given(teamUseCase.create(any())).willReturn(team(childId, "Child", parentId));

        mvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Child", "parentId", parentId.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId", is(parentId.toString())));
    }

    @Test
    void createTeam_blankName_returns400() throws Exception {
        mvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTeam_duplicateName_returns409() throws Exception {
        given(teamUseCase.create(any()))
                .willThrow(new DomainException(new DomainError.Conflict("Team already exists")));

        mvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Duplicate"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createTeam_parentNotFound_returns404() throws Exception {
        given(teamUseCase.create(any()))
                .willThrow(new DomainException(new DomainError.NotFound("Team", UUID.randomUUID())));

        mvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Child", "parentId", UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/teams ────────────────────────────────────────────────────────

    @Test
    void getTeams_defaultTree_returnsList() throws Exception {
        given(teamUseCase.findAll(true)).willReturn(List.of(
                team(UUID.randomUUID(), "A", null),
                team(UUID.randomUUID(), "B", null)));

        mvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getTeams_flatMode_returnsFlatList() throws Exception {
        given(teamUseCase.findAll(false)).willReturn(List.of(
                team(UUID.randomUUID(), "A", null)));

        mvc.perform(get("/api/teams").param("tree", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getTeamById_existing_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(teamUseCase.findById(id)).willReturn(team(id, "Alpha", null));

        mvc.perform(get("/api/teams/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alpha")));
    }

    @Test
    void getTeamById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(teamUseCase.findById(id))
                .willThrow(new DomainException(new DomainError.NotFound("Team", id)));

        mvc.perform(get("/api/teams/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/teams/{id} ───────────────────────────────────────────────────

    @Test
    void updateTeam_validRequest_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        given(teamUseCase.update(any())).willReturn(team(id, "NewName", null));

        mvc.perform(put("/api/teams/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "NewName"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("NewName")));
    }

    @Test
    void updateTeam_blankName_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        mvc.perform(put("/api/teams/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTeam_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        given(teamUseCase.update(any()))
                .willThrow(new DomainException(new DomainError.NotFound("Team", id)));

        mvc.perform(put("/api/teams/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "X"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTeam_duplicateName_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        given(teamUseCase.update(any()))
                .willThrow(new DomainException(new DomainError.Conflict("Name taken")));

        mvc.perform(put("/api/teams/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Taken"}
                                """))
                .andExpect(status().isConflict());
    }

    // ── DELETE /api/teams/{id} ────────────────────────────────────────────────

    @Test
    void deleteTeam_existing_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        willDoNothing().given(teamUseCase).delete(id);

        mvc.perform(delete("/api/teams/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTeam_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        willThrow(new DomainException(new DomainError.NotFound("Team", id)))
                .given(teamUseCase).delete(id);

        mvc.perform(delete("/api/teams/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTeam_hasChildren_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        willThrow(new DomainException(new DomainError.BusinessRule("Cannot delete team with sub-teams")))
                .given(teamUseCase).delete(id);

        mvc.perform(delete("/api/teams/{id}", id))
                .andExpect(status().isBadRequest());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private static Team team(UUID id, String name, UUID parentId) {
        return Team.builder()
                .id(id).name(name).parentId(parentId)
                .children(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
