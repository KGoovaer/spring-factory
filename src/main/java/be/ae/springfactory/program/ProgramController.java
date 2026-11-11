package be.ae.springfactory.program;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/programs")
class ProgramController {

  private final Programs programs;

  ProgramController(Programs programs) {
    this.programs = programs;
  }

  @PostMapping
  ResponseEntity<ProgramResponse> create(@Valid @RequestBody CreateProgramRequest request) {
    Program program = programs.create(request.name());
    return ResponseEntity.created(URI.create("/api/programs/" + program.id()))
        .body(ProgramResponse.of(program, programs::equipment));
  }

  @GetMapping("/{id}")
  ProgramResponse get(@PathVariable UUID id) {
    return ProgramResponse.of(programs.get(id), programs::equipment);
  }

  @PostMapping("/{id}/weeks")
  ResponseEntity<WeekResponse> addWeek(@PathVariable UUID id) {
    Week week = programs.addWeek(id);
    return ResponseEntity.status(HttpStatus.CREATED).body(WeekResponse.of(week, programs::equipment));
  }
}
