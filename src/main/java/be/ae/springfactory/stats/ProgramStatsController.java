package be.ae.springfactory.stats;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Serves the stats of every Week of a Program in one response, as docs/adr/0002 says. */
@RestController
class ProgramStatsController {

  private final WeekStatistics statistics;

  ProgramStatsController(WeekStatistics statistics) {
    this.statistics = statistics;
  }

  @GetMapping("/api/programs/{id}/stats")
  ProgramStatsResponse get(@PathVariable UUID id) {
    return ProgramStatsResponse.of(statistics.ofProgram(id));
  }
}
