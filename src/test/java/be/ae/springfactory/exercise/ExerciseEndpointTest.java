package be.ae.springfactory.exercise;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ExerciseEndpointTest {

  @Autowired private MockMvc mvc;

  @Test
  void listsExercisesAsABareJsonArrayWithWhatEachIsDoneWith() throws Exception {
    mvc.perform(get("/api/exercises"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(12)))
        .andExpect(jsonPath("$[0].id").value("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01"))
        .andExpect(jsonPath("$[0].name").value("Swing"))
        .andExpect(jsonPath("$[0].bodyweight").value(false))
        .andExpect(jsonPath("$[0].hold").value(false))
        .andExpect(jsonPath("$[0].timed").value(false))
        .andExpect(jsonPath("$[0].gear").isEmpty())
        .andExpect(jsonPath("$[0].equipment").value("Kettlebell"))
        .andExpect(jsonPath("$[4].name").value("Snatch"))
        .andExpect(jsonPath("$[7].name").value("Push-up"))
        .andExpect(jsonPath("$[7].bodyweight").value(true))
        .andExpect(jsonPath("$[7].equipment").value("Body weight"))
        .andExpect(jsonPath("$[8].name").value("Plank"))
        .andExpect(jsonPath("$[8].hold").value(true))
        .andExpect(jsonPath("$[11].name").value("Skipping"))
        .andExpect(jsonPath("$[11].timed").value(true))
        .andExpect(jsonPath("$[11].gear").value("Jump rope"))
        .andExpect(jsonPath("$[0].length()").value(7));
  }

  @Test
  void aLifterAddsAnExerciseThatIsThenListed() throws Exception {
    String name = "Battle ropes " + UUID.randomUUID();

    mvc.perform(
            post("/api/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"%s\",\"bodyweight\":true,\"timed\":true,\"gear\":\"Rope\"}"
                        .formatted(name)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/exercises/")))
        .andExpect(jsonPath("$.name").value(name))
        .andExpect(jsonPath("$.hold").value(false))
        .andExpect(jsonPath("$.timed").value(true))
        .andExpect(jsonPath("$.equipment").value("Rope"));

    mvc.perform(get("/api/exercises"))
        .andExpect(jsonPath("$[?(@.name == '%s')].timed".formatted(name)).value(true));
  }

  @Test
  void anExerciseWithATakenNameIsAConflictAndABadOneABadRequest() throws Exception {
    mvc.perform(
            post("/api/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"SWING\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("There is an Exercise named SWING already"));
    mvc.perform(
            post("/api/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hover %s\",\"hold\":true,\"timed\":true}".formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/exercises").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aFieldTheApiDoesNotKnowIsRejected() throws Exception {
    mvc.perform(
            post("/api/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Lunge %s\",\"side\":\"LEFT\"}".formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void aLifterRenamesAnExerciseWhichKeepsItsClassification() throws Exception {
    String id = added("Plank hold " + UUID.randomUUID(), true, true);
    String name = "Side plank " + UUID.randomUUID();

    mvc.perform(
            put("/api/exercises/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\"}".formatted(name)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value(name))
        .andExpect(jsonPath("$.equipment").value("Body weight"))
        .andExpect(jsonPath("$.hold").value(true));

    mvc.perform(get("/api/exercises"))
        .andExpect(jsonPath("$[?(@.id == '%s')].name".formatted(id)).value(name));
  }

  @Test
  void aRenameThatTriesToChangeTheClassificationIsABadRequest() throws Exception {
    String id = added("Halo " + UUID.randomUUID(), false, false);

    mvc.perform(
            put("/api/exercises/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Halo\",\"bodyweight\":true}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/exercises"))
        .andExpect(jsonPath("$[?(@.id == '%s')].equipment".formatted(id)).value("Kettlebell"));
  }

  @Test
  void aRenameToATakenNameIsAConflict() throws Exception {
    String id = added("Halo " + UUID.randomUUID(), false, false);

    mvc.perform(
            put("/api/exercises/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"swing\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void aRenameWithoutANameIsABadRequest() throws Exception {
    String id = added("Halo " + UUID.randomUUID(), false, false);

    mvc.perform(put("/api/exercises/" + id).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aRenameOfAnUnknownExerciseIsNotFound() throws Exception {
    mvc.perform(
            put("/api/exercises/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Halo\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void aRenameOrRemovalOfAMalformedIdIsABadRequest() throws Exception {
    mvc.perform(
            put("/api/exercises/not-an-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Halo\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/exercises/not-an-id")).andExpect(status().isBadRequest());
  }

  @Test
  void aLifterRemovesAnExerciseThatIsThenNoLongerListed() throws Exception {
    String id = added("Windmill " + UUID.randomUUID(), false, false);

    mvc.perform(delete("/api/exercises/" + id)).andExpect(status().isNoContent());

    mvc.perform(get("/api/exercises"))
        .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id)).isEmpty());
    mvc.perform(delete("/api/exercises/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void anExercisePrescribedInAProgramCannotBeRemoved() throws Exception {
    String id = added("Bear crawl " + UUID.randomUUID(), true, false);
    String sessionId = newSessionId();

    mvc.perform(
            post("/api/sessions/{id}/prescriptions", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"exerciseId\":\"%s\",\"sets\":3,\"reps\":10}".formatted(id)))
        .andExpect(status().isCreated());

    mvc.perform(delete("/api/exercises/" + id))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(get("/api/exercises"))
        .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(id)).isNotEmpty());
  }

  @Test
  void anExerciseInALoggedSessionCannotBeRemoved() throws Exception {
    String id = added("Crab walk " + UUID.randomUUID(), true, false);
    String sessionId = newSessionId();

    mvc.perform(
            post("/api/sessions/{id}/log", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-11-11","sets":[{"exerciseId":"%s","reps":10}]}
                    """
                        .formatted(id)))
        .andExpect(status().isCreated());

    mvc.perform(delete("/api/exercises/" + id)).andExpect(status().isConflict());
  }

  @Test
  void aNameLongerThanTheCatalogKeepsIsABadRequest() throws Exception {
    mvc.perform(
            post("/api/exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\"}".formatted("x".repeat(201))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  private String newSessionId() throws Exception {
    String programId =
        read(
            mvc.perform(
                post("/api/programs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Giant\"}")),
            "$.id");
    String weekId = read(mvc.perform(post("/api/programs/{id}/weeks", programId)), "$.id");
    return read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)), "$.id");
  }

  private static String read(ResultActions result, String path) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
  }

  private String added(String name, boolean bodyweight, boolean hold) throws Exception {
    String body =
        mvc.perform(
                post("/api/exercises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"%s\",\"bodyweight\":%s,\"hold\":%s}"
                            .formatted(name, bodyweight, hold)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }
}
