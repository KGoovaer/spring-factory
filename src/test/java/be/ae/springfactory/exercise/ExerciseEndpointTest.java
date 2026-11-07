package be.ae.springfactory.exercise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
  void listsExercisesAsABareJsonArrayOfIdNameAndBodyweight() throws Exception {
    mvc.perform(get("/api/exercises"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(8))
        .andExpect(jsonPath("$[0].id").value("8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01"))
        .andExpect(jsonPath("$[0].name").value("Swing"))
        .andExpect(jsonPath("$[4].name").value("Snatch"))
        .andExpect(jsonPath("$[0].bodyweight").value(false))
        .andExpect(jsonPath("$[5].name").value("Push-up"))
        .andExpect(jsonPath("$[5].bodyweight").value(true))
        .andExpect(jsonPath("$[0].length()").value(3));
  }
}
