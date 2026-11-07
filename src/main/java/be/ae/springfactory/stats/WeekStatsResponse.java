package be.ae.springfactory.stats;

import java.math.BigDecimal;
import java.util.UUID;

/** A Week's stats as JSON. Adherence is a percentage and is null for a Week without Sessions. */
record WeekStatsResponse(
    UUID weekId, BigDecimal plannedVolume, BigDecimal loggedVolume, BigDecimal adherence) {

  static WeekStatsResponse of(WeekStats stats) {
    return new WeekStatsResponse(
        stats.weekId(), stats.plannedVolume(), stats.loggedVolume(), stats.adherence());
  }
}
