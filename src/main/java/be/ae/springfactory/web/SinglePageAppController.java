package be.ae.springfactory.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lets the browser open a frontend route directly: the React app in {@code frontend/} is served as
 * static resources, and its routes are only known to its client-side router, so they all get its
 * {@code index.html}.
 */
@Controller
class SinglePageAppController {

  @GetMapping({
    "/programs/{id}",
    "/programs/{id}/setup",
    "/programs/{id}/sessions/{sessionId}/log"
  })
  String index() {
    return "forward:/index.html";
  }
}
