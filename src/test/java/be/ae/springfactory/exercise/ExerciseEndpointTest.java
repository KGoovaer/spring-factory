package be.ae.springfactory.exercise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
  void getExercisesReturnsTheCatalogAsAJsonArray() throws Exception {
    mvc.perform(get("/api/exercises"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
                    [
                      {"id": "swing", "name": "Swing"},
                      {"id": "goblet-squat", "name": "Goblet squat"},
                      {"id": "clean-and-press", "name": "Clean and press"},
                      {"id": "turkish-get-up", "name": "Turkish get-up"},
                      {"id": "snatch", "name": "Snatch"}
                    ]
                    """,
                    true));
  }
}
