package be.ae.springfactory.program;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class SessionEndpointTest {

  private static final String SWING = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01";

  @Autowired private MockMvc mvc;

  @Test
  void addsSessionsWithPrescriptionsThatTheProgramShows() throws Exception {
    String programId = read(mvc.perform(createProgram()), "$.id");
    String weekId = read(mvc.perform(post("/api/programs/{id}/weeks", programId)), "$.id");

    String sessionId =
        read(
            mvc.perform(post("/api/weeks/{id}/sessions", weekId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prescriptions.length()").value(0)),
            "$.id");
    mvc.perform(
            addPrescription(
                sessionId,
                "{\"exerciseId\":\"%s\",\"sets\":10,\"reps\":10,\"bellKg\":24,\"restSeconds\":60}"
                    .formatted(SWING)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.exerciseId").value(SWING))
        .andExpect(jsonPath("$.sets").value(10))
        .andExpect(jsonPath("$.reps").value(10))
        .andExpect(jsonPath("$.bellKg").value(24))
        .andExpect(jsonPath("$.restSeconds").value(60));
    mvc.perform(
            addPrescription(
                sessionId,
                "{\"exerciseId\":\"%s\",\"sets\":5,\"reps\":1,\"bellKg\":10.50}".formatted(SWING)))
        .andExpect(status().isCreated());

    String program =
        mvc.perform(get("/api/programs/{id}", programId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weeks[0].id").value(weekId))
            .andExpect(jsonPath("$.weeks[0].sessions.length()").value(1))
            .andExpect(jsonPath("$.weeks[0].sessions[0].id").value(sessionId))
            .andExpect(jsonPath("$.weeks[0].sessions[0].prescriptions.length()").value(2))
            .andExpect(jsonPath("$.weeks[0].sessions[0].prescriptions[0].sets").value(10))
            .andExpect(jsonPath("$.weeks[0].sessions[0].prescriptions[1].restSeconds").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(program).contains("\"bellKg\":10.50");
  }

  @Test
  void anUnknownWeekSessionOrExerciseIsNotFound() throws Exception {
    mvc.perform(post("/api/weeks/{id}/sessions", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(addPrescription(UUID.randomUUID().toString(), swings("\"bellKg\":16")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

    String sessionId = newSessionId();
    mvc.perform(
            addPrescription(
                sessionId,
                "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":1,\"bellKg\":16}"
                    .formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value(startsWith("No Exercise")));
  }

  @Test
  void rejectsAPrescriptionOutsideItsLimits() throws Exception {
    String sessionId = newSessionId();

    for (String body :
        new String[] {
          "{\"sets\":1,\"reps\":1,\"bellKg\":16}",
          "{\"exerciseId\":\"%s\",\"sets\":0,\"reps\":1,\"bellKg\":16}".formatted(SWING),
          "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":0,\"bellKg\":16}".formatted(SWING),
          "{\"exerciseId\":\"%s\",\"reps\":1,\"bellKg\":16}".formatted(SWING),
          swings("\"bellKg\":0"),
          swings("\"bellKg\":-4"),
          swings("\"bellKg\":16,\"restSeconds\":-1")
        }) {
      mvc.perform(addPrescription(sessionId, body))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
    mvc.perform(addPrescription(sessionId, swings("\"bellKg\":16,\"restSeconds\":0")))
        .andExpect(status().isCreated());
  }

  private static String swings(String rest) {
    return "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":1,%s}".formatted(SWING, rest);
  }

  private String newSessionId() throws Exception {
    String programId = read(mvc.perform(createProgram()), "$.id");
    String weekId = read(mvc.perform(post("/api/programs/{id}/weeks", programId)), "$.id");
    return read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)), "$.id");
  }

  private static RequestBuilder createProgram() {
    return post("/api/programs")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Simple & Sinister\"}");
  }

  private static RequestBuilder addPrescription(String sessionId, String body) {
    return post("/api/sessions/{id}/prescriptions", sessionId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private static String read(ResultActions result, String path)
      throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
  }
}
