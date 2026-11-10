package be.ae.springfactory.exercise;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
class ExerciseController {

  private final ExerciseCatalog catalog;

  ExerciseController(ExerciseCatalog catalog) {
    this.catalog = catalog;
  }

  @GetMapping
  List<ExerciseResponse> all() {
    return catalog.all().stream().map(ExerciseResponse::of).toList();
  }

  @PostMapping
  ResponseEntity<ExerciseResponse> add(@Valid @RequestBody AddExerciseRequest request) {
    Exercise exercise =
        catalog.add(
            request.name(),
            Boolean.TRUE.equals(request.bodyweight()),
            Boolean.TRUE.equals(request.hold()),
            Boolean.TRUE.equals(request.timed()),
            request.gear());
    return ResponseEntity.created(URI.create("/api/exercises/" + exercise.id()))
        .body(ExerciseResponse.of(exercise));
  }
}
