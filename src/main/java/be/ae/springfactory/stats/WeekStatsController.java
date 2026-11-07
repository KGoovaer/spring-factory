package be.ae.springfactory.stats;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the stats of a Week, addressed by its id as docs/adr/0001-exercise-ids-are-uuids.md says,
 * and of all Weeks of a Program in one response.
 */
@RestController
class WeekStatsController {

  private final WeekStatistics statistics;

  WeekStatsController(WeekStatistics statistics) {
    this.statistics = statistics;
  }

  @GetMapping("/api/weeks/{id}/stats")
  WeekStatsResponse get(@PathVariable UUID id) {
    return WeekStatsResponse.of(statistics.of(id));
  }

  @GetMapping("/api/programs/{id}/stats")
  ProgramStatsResponse getProgram(@PathVariable UUID id) {
    return ProgramStatsResponse.of(statistics.ofProgram(id));
  }
}
