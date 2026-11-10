package be.ae.springfactory.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SinglePageAppTest {

  @Autowired private MockMvc mvc;

  @Test
  void aProgramPageOpenedDirectlyGetsTheFrontend() throws Exception {
    mvc.perform(get("/programs/{id}", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void theLogSessionPageOpenedDirectlyGetsTheFrontend() throws Exception {
    mvc.perform(get("/programs/{id}/sessions/{sessionId}/log", UUID.randomUUID(), UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }
}
