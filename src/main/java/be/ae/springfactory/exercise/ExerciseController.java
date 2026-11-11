package be.ae.springfactory.exercise;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
  ResponseEntity<ExerciseResponse> add(@Valid @RequestBody ExerciseRequest request) {
    Exercise exercise = request.addTo(catalog);
    return ResponseEntity.created(URI.create("/api/exercises/" + exercise.id()))
        .body(ExerciseResponse.of(exercise));
  }

  @PutMapping("/{id}")
  ExerciseResponse change(@PathVariable UUID id, @Valid @RequestBody ExerciseRequest request) {
    return ExerciseResponse.of(request.changeIn(catalog, id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void remove(@PathVariable UUID id) {
    catalog.remove(id);
  }
}
