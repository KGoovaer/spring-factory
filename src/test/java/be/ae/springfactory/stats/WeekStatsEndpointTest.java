package be.ae.springfactory.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class WeekStatsEndpointTest {

  private static final String SWING = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01";

  @Autowired private MockMvc mvc;

  @Test
  void servesVolumeAndAdherenceOfAWeek() throws Exception {
    String weekId = newWeekId();
    String logged = read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)), "$.id");
    String skipped = read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)), "$.id");
    String unlogged = read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)), "$.id");
    for (String session : new String[] {logged, skipped, unlogged}) {
      mvc.perform(
              post("/api/sessions/{id}/prescriptions", session)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"exerciseId\":\"%s\",\"sets\":10,\"reps\":10,\"bellKg\":24.00}"
                          .formatted(SWING)))
          .andExpect(status().isCreated());
    }
    mvc.perform(
            post("/api/sessions/{id}/log", logged)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":50,\"bellKg\":20}]}"
                        .formatted(SWING)))
        .andExpect(status().isCreated());

    String json =
        mvc.perform(get("/api/weeks/{id}/stats", weekId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weekId").value(weekId))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(json)
        .contains("\"plannedVolume\":7200")
        .contains("\"loggedVolume\":1000")
        .contains("\"adherence\":33.33");
  }

  @Test
  void servesTheStatsOfEveryWeekOfAProgram() throws Exception {
    String programId = newProgramId();
    String weekId = read(mvc.perform(post("/api/programs/{id}/weeks", programId)), "$.id");
    String sessionId = read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)), "$.id");
    mvc.perform(
            post("/api/sessions/{id}/prescriptions", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"exerciseId\":\"%s\",\"sets\":10,\"reps\":10,\"bellKg\":24.00}"
                        .formatted(SWING)))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/sessions/{id}/log", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":50,\"bellKg\":20}]}"
                        .formatted(SWING)))
        .andExpect(status().isCreated());
    String emptyWeekId = read(mvc.perform(post("/api/programs/{id}/weeks", programId)), "$.id");

    String json =
        mvc.perform(get("/api/programs/{id}/stats", programId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.programId").value(programId))
            .andExpect(jsonPath("$.weeks[0].weekId").value(weekId))
            .andExpect(jsonPath("$.weeks[0].number").value(1))
            .andExpect(jsonPath("$.weeks[0].sessions[0].sessionId").value(sessionId))
            .andExpect(jsonPath("$.weeks[0].sessions[0].logged").value(true))
            .andExpect(jsonPath("$.weeks[1].weekId").value(emptyWeekId))
            .andExpect(jsonPath("$.weeks[1].sessions").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(json)
        .contains(
            "\"plannedVolume\":2400,\"loggedVolume\":1000,\"adherence\":100,\"sessions\":[{\"sessionId\":\"%s\",\"plannedVolume\":2400,\"logged\":true}]"
                .formatted(sessionId))
        .contains("\"adherence\":null");
  }

  @Test
  void anUnknownProgramHasNoStats() throws Exception {
    mvc.perform(get("/api/programs/{id}/stats", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void anEmptyWeekHasNullAdherence() throws Exception {
    String json =
        mvc.perform(get("/api/weeks/{id}/stats", newWeekId()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(json)
        .contains("\"plannedVolume\":0")
        .contains("\"loggedVolume\":0")
        .contains("\"adherence\":null");
  }

  @Test
  void anUnknownWeekIsNotFound() throws Exception {
    mvc.perform(get("/api/weeks/{id}/stats", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  private String newWeekId() throws Exception {
    return read(mvc.perform(post("/api/programs/{id}/weeks", newProgramId())), "$.id");
  }

  private String newProgramId() throws Exception {
    return read(
        mvc.perform(
            post("/api/programs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Giant\"}")),
        "$.id");
  }

  private static String read(ResultActions result, String path) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
  }
}
