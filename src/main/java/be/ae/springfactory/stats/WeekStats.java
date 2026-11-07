package be.ae.springfactory.stats;

import be.ae.springfactory.log.LoggedSession;
import be.ae.springfactory.program.Session;
import be.ae.springfactory.program.Week;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Volume and Adherence of one Week, derived from its planned Sessions and their Logged Sessions and
 * never stored, with the {@link SessionStats} of each Session in the Week's order.
 *
 * <ul>
 *   <li>{@code plannedVolume}: Sets × Reps × Bell weight, summed over the Week's Prescriptions.
 *   <li>{@code loggedVolume}: Reps × Bell weight, summed over every logged Set of the Week's
 *       Sessions, including Exercises the Session did not prescribe.
 *   <li>{@code adherence}: the percentage of the Week's Sessions that have a Logged Session, rounded
 *       half up to 2 decimals ({@code 66.67}); {@code null} for a Week without Sessions. A Logged
 *       Session counts whatever its date.
 * </ul>
 *
 * All three are normalised: no trailing zeros and never in exponent form ({@code 100}, not {@code
 * 100.00} or {@code 1E+2}).
 */
public record WeekStats(
    UUID weekId,
    int number,
    BigDecimal plannedVolume,
    BigDecimal loggedVolume,
    BigDecimal adherence,
    List<SessionStats> sessions) {

  public WeekStats {
    sessions = List.copyOf(sessions);
  }

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Calculates the stats of the Week from these Logged Sessions. Logged Sessions of Sessions outside
   * the Week are ignored.
   */
  static WeekStats of(Week week, Collection<LoggedSession> loggedSessions) {
    Set<UUID> sessionIds = week.sessions().stream().map(Session::id).collect(Collectors.toSet());
    List<LoggedSession> logged =
        loggedSessions.stream().filter(log -> sessionIds.contains(log.sessionId())).toList();

    BigDecimal plannedVolume =
        week.sessions().stream().map(Session::volume).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal loggedVolume =
        logged.stream().map(LoggedSession::volume).reduce(BigDecimal.ZERO, BigDecimal::add);
    Set<UUID> loggedSessionIds =
        logged.stream().map(LoggedSession::sessionId).collect(Collectors.toSet());
    List<SessionStats> sessions =
        week.sessions().stream()
            .map(
                session ->
                    new SessionStats(
                        session.id(),
                        normalised(session.volume()),
                        loggedSessionIds.contains(session.id())))
            .toList();

    return new WeekStats(
        week.id(),
        week.number(),
        normalised(plannedVolume),
        normalised(loggedVolume),
        adherence(loggedSessionIds.size(), sessionIds.size()),
        sessions);
  }

  private static BigDecimal adherence(long logged, int planned) {
    if (planned == 0) {
      return null;
    }
    return normalised(
        BigDecimal.valueOf(logged)
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(planned), 2, RoundingMode.HALF_UP));
  }

  private static BigDecimal normalised(BigDecimal value) {
    BigDecimal stripped = value.stripTrailingZeros();
    return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
  }
}
