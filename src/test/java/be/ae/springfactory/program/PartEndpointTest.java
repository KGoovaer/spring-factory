package be.ae.springfactory.program;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class PartEndpointTest {

  private static final String SWING = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01";
  private static final String WAITER_PRESS = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c09";
  private static final String SKIPPING = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c0c";

  @Autowired private MockMvc mvc;

  @Test
  void setsUpASessionOfPartsWithNotesThatTheProgramShows() throws Exception {
    String programId = read(post("/api/programs").contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Kettlebell EMOM\"}"), "$.id");
    String weekId = read(post("/api/programs/{id}/weeks", programId), "$.id");
    String sessionId = read(post("/api/weeks/{id}/sessions", weekId), "$.id");

    mvc.perform(json(post("/api/sessions/{id}/parts", sessionId),
            "{\"format\":\"EMOM\",\"minutes\":20,\"rotation\":4}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.parts[0].number").value(1))
        .andExpect(jsonPath("$.parts[0].format").value("EMOM"))
        .andExpect(jsonPath("$.parts[0].minutes").value(20))
        .andExpect(jsonPath("$.parts[0].rotation").value(4))
        .andExpect(jsonPath("$.parts[0].rounds").isEmpty());
    mvc.perform(json(post("/api/sessions/{id}/parts/1/prescriptions", sessionId),
            "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":5,\"side\":\"RIGHT\",\"bellKg\":16,\"minute\":1}"
                .formatted(SWING)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.side").value("RIGHT"))
        .andExpect(jsonPath("$.minute").value(1));
    mvc.perform(json(post("/api/sessions/{id}/parts/1/prescriptions", sessionId),
            "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":1,\"workSeconds\":45,\"minute\":3}"
                .formatted(SKIPPING)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.workSeconds").value(45));
    mvc.perform(json(post("/api/sessions/{id}/parts", sessionId),
            "{\"format\":\"REST\",\"restSeconds\":60}"))
        .andExpect(status().isCreated());
    mvc.perform(json(post("/api/sessions/{id}/parts", sessionId),
            "{\"format\":\"ROUNDS\",\"rounds\":3}"))
        .andExpect(status().isCreated());
    mvc.perform(json(post("/api/sessions/{id}/parts/3/prescriptions", sessionId),
            "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":8,\"side\":\"EACH\",\"bellKg\":12}"
                .formatted(WAITER_PRESS)))
        .andExpect(status().isCreated());
    mvc.perform(json(put("/api/sessions/{id}/notes", sessionId),
            "{\"notes\":\"Keep intensity at 70-80%.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("Keep intensity at 70-80%."));

    mvc.perform(get("/api/programs/{id}", programId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weeks[0].sessions[0].notes").value("Keep intensity at 70-80%."))
        .andExpect(jsonPath("$.weeks[0].sessions[0].equipment[0]").value("Jump rope"))
        .andExpect(jsonPath("$.weeks[0].sessions[0].equipment[1]").value("Kettlebell"))
        .andExpect(jsonPath("$.weeks[0].sessions[0].parts.length()").value(3))
        .andExpect(jsonPath("$.weeks[0].sessions[0].parts[1].restSeconds").value(60))
        .andExpect(jsonPath("$.weeks[0].sessions[0].parts[2].rounds").value(3))
        .andExpect(jsonPath("$.weeks[0].sessions[0].parts[2].prescriptions[0].side").value("EACH"));

    // EMOM: 5 × 5 × 16 = 400 kg; Rounds: 3 × (8 + 8) × 12 = 576 kg.
    mvc.perform(get("/api/programs/{id}/stats", programId))
        .andExpect(jsonPath("$.weeks[0].sessions[0].plannedVolume").value(976));
  }

  @Test
  void rejectsAPartOrPrescriptionThatDoesNotFit() throws Exception {
    String programId = read(post("/api/programs").contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Kettlebell EMOM\"}"), "$.id");
    String weekId = read(post("/api/programs/{id}/weeks", programId), "$.id");
    String sessionId = read(post("/api/weeks/{id}/sessions", weekId), "$.id");

    for (String body :
        new String[] {
          "{}",
          "{\"format\":\"ROUNDS\"}",
          "{\"format\":\"EMOM\",\"minutes\":20}",
          "{\"format\":\"REST\",\"restSeconds\":0}",
          "{\"format\":\"SETS\",\"rounds\":2}",
          "{\"format\":\"TABATA\"}",
          "{\"format\":\"REST\",\"restSeconds\":60,\"title\":\"Breather\"}"
        }) {
      mvc.perform(json(post("/api/sessions/{id}/parts", sessionId), body))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
    mvc.perform(json(post("/api/sessions/{id}/parts/1/prescriptions", sessionId),
            "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":5,\"bellKg\":16}".formatted(SWING)))
        .andExpect(status().isNotFound());
    mvc.perform(json(post("/api/sessions/{id}/prescriptions", sessionId),
            "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":5,\"bellKg\":16,\"notes\":\"heavy\"}"
                .formatted(SWING)))
        .andExpect(status().isBadRequest());
    mvc.perform(json(post("/api/sessions/{id}/prescriptions", sessionId),
            "{\"exerciseId\":\"%s\",\"sets\":1,\"reps\":5,\"bellKg\":16,\"side\":\"UP\"}"
                .formatted(SWING)))
        .andExpect(status().isBadRequest());
  }

  private static RequestBuilder json(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      String body) {
    return request.contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private String read(RequestBuilder request, String path) throws Exception {
    return JsonPath.read(mvc.perform(request).andReturn().getResponse().getContentAsString(), path);
  }
}
