package be.ae.springfactory.exercise;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
}
