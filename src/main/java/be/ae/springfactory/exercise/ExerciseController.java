package be.ae.springfactory.exercise;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}
