package be.ae.springfactory.log;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Logs a Session as performed, corrects that log and reads it back. */
@RestController
class LoggedSessionController {

  private final LoggedSessions loggedSessions;

  LoggedSessionController(LoggedSessions loggedSessions) {
    this.loggedSessions = loggedSessions;
  }

  @PostMapping("/api/sessions/{id}/log")
  ResponseEntity<LoggedSessionResponse> log(
      @PathVariable UUID id, @Valid @RequestBody LogSessionRequest request) {
    LoggedSession logged = loggedSessions.log(id, request.date(), request.toLoggedSets());
    return ResponseEntity.status(HttpStatus.CREATED).body(LoggedSessionResponse.of(logged));
  }

  @PutMapping("/api/sessions/{id}/log")
  LoggedSessionResponse correct(
      @PathVariable UUID id, @Valid @RequestBody LogSessionRequest request) {
    return LoggedSessionResponse.of(
        loggedSessions.correct(id, request.date(), request.toLoggedSets()));
  }

  @GetMapping("/api/sessions/{id}/log")
  LoggedSessionResponse get(@PathVariable UUID id) {
    return LoggedSessionResponse.of(loggedSessions.get(id));
  }
}
