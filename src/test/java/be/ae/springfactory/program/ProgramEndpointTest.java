package be.ae.springfactory.program;

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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class ProgramEndpointTest {

  @Autowired private MockMvc mvc;

  @Test
  void createsAProgramAndPointsToItWithALocation() throws Exception {
    MockHttpServletResponse response =
        mvc.perform(createProgram("{\"name\":\"Simple & Sinister\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Simple & Sinister"))
            .andExpect(jsonPath("$.weeks.length()").value(0))
            .andReturn()
            .getResponse();
    String id = JsonPath.read(response.getContentAsString(), "$.id");

    assertThat(response.getHeader("Location")).isEqualTo("/api/programs/" + id);
    mvc.perform(get(response.getHeader("Location")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Simple & Sinister"));
  }

  @Test
  void returnsTheProgramWithItsWeeksInOrder() throws Exception {
    String id = createdProgramId("Giant");

    mvc.perform(post("/api/programs/{id}/weeks", id))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.number").value(1));
    mvc.perform(post("/api/programs/{id}/weeks", id))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.number").value(2));

    mvc.perform(get("/api/programs/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("Giant"))
        .andExpect(jsonPath("$.weeks.length()").value(2))
        .andExpect(jsonPath("$.weeks[0].number").value(1))
        .andExpect(jsonPath("$.weeks[1].number").value(2))
        .andExpect(jsonPath("$.weeks[0].length()").value(1));
  }

  @Test
  void rejectsABlankName() throws Exception {
    mvc.perform(createProgram("{\"name\":\"  \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(createProgram("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void anUnknownProgramIsNotFound() throws Exception {
    UUID unknown = UUID.randomUUID();
    mvc.perform(get("/api/programs/{id}", unknown))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    mvc.perform(post("/api/programs/{id}/weeks", unknown)).andExpect(status().isNotFound());
  }

  private static RequestBuilder createProgram(String body) {
    return post("/api/programs").contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private String createdProgramId(String name) throws Exception {
    String body =
        mvc.perform(createProgram("{\"name\":\"" + name + "\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.id");
  }
}
