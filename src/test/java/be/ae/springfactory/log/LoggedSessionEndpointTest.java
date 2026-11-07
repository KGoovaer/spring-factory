package be.ae.springfactory.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
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
class LoggedSessionEndpointTest {

  private static final String SWING = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01";
  private static final String SNATCH = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c05";
  private static final String PUSH_UP = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c06";

  @Autowired private MockMvc mvc;

  @Test
  void logsASessionAndReadsItBack() throws Exception {
    String sessionId = newSessionId();
    String body =
        """
        {"date":"2026-11-07","sets":[
          {"exerciseId":"%s","reps":10,"bellKg":24},
          {"exerciseId":"%s","reps":5,"bellKg":10.50}]}
        """
            .formatted(SWING, SNATCH);

    mvc.perform(log(sessionId, body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sessionId").value(sessionId))
        .andExpect(jsonPath("$.date").value("2026-11-07"))
        .andExpect(jsonPath("$.sets.length()").value(2))
        .andExpect(jsonPath("$.sets[0].exerciseId").value(SWING))
        .andExpect(jsonPath("$.sets[0].reps").value(10))
        .andExpect(jsonPath("$.sets[0].bellKg").value(24));

    String logged =
        mvc.perform(get("/api/sessions/{id}/log", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").value("2026-11-07"))
            .andExpect(jsonPath("$.sets[1].exerciseId").value(SNATCH))
            .andExpect(jsonPath("$.sets[1].reps").value(5))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(logged).contains("\"bellKg\":10.50");
  }

  @Test
  void loggingDoesNotChangeTheProgram() throws Exception {
    String programId = read(mvc.perform(createProgram()), "$.id");
    String weekId = read(mvc.perform(post("/api/programs/{id}/weeks", programId)), "$.id");
    String sessionId = read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)), "$.id");
    mvc.perform(
            post("/api/sessions/{id}/prescriptions", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"exerciseId\":\"%s\",\"sets\":10,\"reps\":10,\"bellKg\":24}"
                        .formatted(SWING)))
        .andExpect(status().isCreated());
    String before = programJson(programId);

    mvc.perform(log(sessionId, oneSet("\"bellKg\":32"))).andExpect(status().isCreated());

    assertThat(programJson(programId)).isEqualTo(before);
  }

  @Test
  void aSecondLogOfTheSameSessionIsAConflict() throws Exception {
    String sessionId = newSessionId();
    mvc.perform(log(sessionId, oneSet("\"bellKg\":24"))).andExpect(status().isCreated());

    mvc.perform(log(sessionId, oneSet("\"bellKg\":32")))
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(get("/api/sessions/{id}/log", sessionId))
        .andExpect(jsonPath("$.sets[0].bellKg").value(24));
  }

  @Test
  void anUnknownSessionExerciseOrLogIsNotFound() throws Exception {
    String unknown = UUID.randomUUID().toString();
    mvc.perform(log(unknown, oneSet("\"bellKg\":24")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(get("/api/sessions/{id}/log", unknown))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

    String sessionId = newSessionId();
    mvc.perform(get("/api/sessions/{id}/log", sessionId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(
            log(
                sessionId,
                "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":1,\"bellKg\":16}]}"
                    .formatted(UUID.randomUUID())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value(startsWith("No Exercise")));
  }

  @Test
  void rejectsALogOutsideItsLimits() throws Exception {
    String sessionId = newSessionId();
    String set = "{\"exerciseId\":\"%s\",\"reps\":10,\"bellKg\":24}".formatted(SWING);

    for (String body :
        new String[] {
          "{\"sets\":[%s]}".formatted(set),
          "{\"date\":\"07/11/2026\",\"sets\":[%s]}".formatted(set),
          "{\"date\":\"2026-11-07\"}",
          "{\"date\":\"2026-11-07\",\"sets\":[]}",
          "{\"date\":\"2026-11-07\",\"sets\":[null]}",
          "{\"date\":\"2026-11-07\",\"sets\":[{\"reps\":10,\"bellKg\":24}]}",
          "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"bellKg\":24}]}"
              .formatted(SWING),
          "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":0,\"bellKg\":24}]}"
              .formatted(SWING),
          oneSet("\"bellKg\":0"),
          oneSet("\"bellKg\":-4"),
          "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":10}]}"
              .formatted(SWING)
        }) {
      mvc.perform(log(sessionId, body))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
    mvc.perform(get("/api/sessions/{id}/log", sessionId)).andExpect(status().isNotFound());
  }

  @Test
  void logsABodyweightSetWithoutABellAndAnswersWithANullBell() throws Exception {
    mvc.perform(
            log(
                newSessionId(),
                "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":20}]}"
                    .formatted(PUSH_UP)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sets[0].exerciseId").value(PUSH_UP))
        .andExpect(jsonPath("$.sets[0].bellKg").value(nullValue()));
  }

  @Test
  void rejectsABellOnABodyweightSet() throws Exception {
    String sessionId = newSessionId();

    mvc.perform(
            log(
                sessionId,
                """
                {"date":"2026-11-07","sets":[{"exerciseId":"%s","reps":20,"bellKg":8}]}"""
                    .formatted(PUSH_UP)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(startsWith("Push-up is a bodyweight Exercise")));
    mvc.perform(get("/api/sessions/{id}/log", sessionId)).andExpect(status().isNotFound());
  }

  @Test
  void acceptsAFutureDate() throws Exception {
    mvc.perform(
            log(
                newSessionId(),
                "{\"date\":\"2099-01-01\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":1,\"bellKg\":8}]}"
                    .formatted(SWING)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.date").value("2099-01-01"));
  }

  private static String oneSet(String bell) {
    return "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":10,%s}]}"
        .formatted(SWING, bell);
  }

  private String programJson(String programId) throws Exception {
    return mvc.perform(get("/api/programs/{id}", programId))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private String newSessionId() throws Exception {
    String programId = read(mvc.perform(createProgram()), "$.id");
    String weekId = read(mvc.perform(post("/api/programs/{id}/weeks", programId)), "$.id");
    return read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)), "$.id");
  }

  private static RequestBuilder createProgram() {
    return post("/api/programs")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Giant\"}");
  }

  private static RequestBuilder log(String sessionId, String body) {
    return post("/api/sessions/{id}/log", sessionId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private static String read(ResultActions result, String path) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
  }
}
