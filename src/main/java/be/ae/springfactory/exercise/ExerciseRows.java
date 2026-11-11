package be.ae.springfactory.exercise;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** The rows of the exercise table. Only {@link ExerciseCatalog} uses it. */
interface ExerciseRows extends JpaRepository<ExerciseRow, UUID> {

  /** The listed Exercises, in catalog order. */
  List<ExerciseRow> findByRemovedFalseOrderByPosition();

  /** The row last in the catalog, listed or removed. */
  Optional<ExerciseRow> findTopByOrderByPositionDesc();

  /** Whether another listed Exercise than this id has this name key. */
  boolean existsByNameKeyAndIdNot(String nameKey, UUID id);
}
