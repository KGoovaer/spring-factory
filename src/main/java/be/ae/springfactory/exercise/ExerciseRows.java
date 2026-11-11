package be.ae.springfactory.exercise;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** The rows of the exercise table. Only {@link ExerciseCatalog} uses it. */
interface ExerciseRows extends JpaRepository<ExerciseRow, UUID> {

  /** All rows, in catalog order. */
  List<ExerciseRow> findAllByOrderByPosition();

  /** The row last in the catalog. */
  Optional<ExerciseRow> findTopByOrderByPositionDesc();

  /** Whether an Exercise other than this id has this name key. */
  boolean existsByNameKeyAndIdNot(String nameKey, UUID id);
}
