package be.ae.springfactory.stats;

import java.util.List;
import java.util.UUID;

/** The {@link WeekStats} of every Week of a Program, in Week order. */
public record ProgramStats(UUID programId, List<WeekStats> weeks) {
  public ProgramStats {
    weeks = List.copyOf(weeks);
  }
}
