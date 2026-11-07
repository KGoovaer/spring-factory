package be.ae.springfactory.stats;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A Program's stats as JSON: {programId, weeks: [{weekId, number, plannedVolume, loggedVolume,
 * adherence, sessions: [{sessionId, plannedVolume, logged}]}]}. Adherence is a percentage and is
 * null for a Week without Sessions.
 */
record ProgramStatsResponse(UUID programId, List<Week> weeks) {

  record Week(
      UUID weekId,
      int number,
      BigDecimal plannedVolume,
      BigDecimal loggedVolume,
      BigDecimal adherence,
      List<Session> sessions) {}

  record Session(UUID sessionId, BigDecimal plannedVolume, boolean logged) {}

  static ProgramStatsResponse of(ProgramStats stats) {
    return new ProgramStatsResponse(
        stats.programId(), stats.weeks().stream().map(ProgramStatsResponse::week).toList());
  }

  private static Week week(WeekStats week) {
    return new Week(
        week.weekId(),
        week.number(),
        week.plannedVolume(),
        week.loggedVolume(),
        week.adherence(),
        week.sessions().stream()
            .map(s -> new Session(s.sessionId(), s.plannedVolume(), s.logged()))
            .toList());
  }
}
