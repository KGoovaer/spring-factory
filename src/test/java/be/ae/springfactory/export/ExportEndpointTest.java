package be.ae.springfactory.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ExportEndpointTest {

  private static final String SWING = "8f1c2a4e-3b6d-4c1a-9e57-0d2b7f6a1c01";

  @Autowired private MockMvc mvc;

  @Test
  void downloadsThePlannedProgramsAsACsvFile() throws Exception {
    String name = "Export " + UUID.randomUUID();
    String sessionId = newSession(name);
    mvc.perform(
            post("/api/sessions/{id}/prescriptions", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"exerciseId\":\"%s\",\"sets\":10,\"reps\":10,\"bellKg\":24}"
                        .formatted(SWING)))
        .andExpect(status().isCreated());

    String csv = download("/api/export/planned.csv", "kettlebell-planned.csv");

    assertThat(csv).startsWith("﻿Program,Week,Session,Notes,Part,Format,");
    assertThat(csv).contains("\r\n" + name + ",1,1,,1,Sets,,,,,Swing,10,10,,,,,24,,\r\n");
  }

  @Test
  void downloadsTheLoggedSessionsAsACsvFile() throws Exception {
    String name = "Export " + UUID.randomUUID();
    String sessionId = newSession(name);
    mvc.perform(
            post("/api/sessions/{id}/log", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\"2026-11-07\",\"sets\":[{\"exerciseId\":\"%s\",\"reps\":10,\"bellKg\":24}]}"
                        .formatted(SWING)))
        .andExpect(status().isCreated());

    String csv = download("/api/export/logged.csv", "kettlebell-logged.csv");

    assertThat(csv).startsWith("﻿Program,Week,Session,Date,");
    assertThat(csv).contains("\r\n" + name + ",1,1,2026-11-07,,Swing,10,,,,24,,,\r\n");
  }

  private String download(String path, String filename) throws Exception {
    MvcResult result =
        mvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(
                header().string("Content-Disposition", "attachment; filename=\"" + filename + "\""))
            .andReturn();
    return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  private String newSession(String programName) throws Exception {
    String programId =
        read(
            mvc.perform(
                    post("/api/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(programName)))
                .andReturn());
    String weekId = read(mvc.perform(post("/api/programs/{id}/weeks", programId)).andReturn());
    return read(mvc.perform(post("/api/weeks/{id}/sessions", weekId)).andReturn());
  }

  private static String read(MvcResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
  }
}
