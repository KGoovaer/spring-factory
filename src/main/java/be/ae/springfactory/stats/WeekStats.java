package be.ae.springfactory.stats;

import be.ae.springfactory.log.LoggedSession;
import be.ae.springfactory.log.LoggedSet;
import be.ae.springfactory.program.Prescription;
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
 * never stored.
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
    UUID weekId, BigDecimal plannedVolume, BigDecimal loggedVolume, BigDecimal adherence) {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Calculates the stats of the Week from these Logged Sessions. Logged Sessions of Sessions outside
   * the Week are ignored.
   */
  public static WeekStats of(Week week, Collection<LoggedSession> loggedSessions) {
    Set<UUID> sessionIds = week.sessions().stream().map(Session::id).collect(Collectors.toSet());
    List<LoggedSession> logged =
        loggedSessions.stream().filter(log -> sessionIds.contains(log.sessionId())).toList();

    BigDecimal planned =
        week.sessions().stream()
            .flatMap(session -> session.prescriptions().stream())
            .map(WeekStats::volume)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal done =
        logged.stream()
            .flatMap(log -> log.sets().stream())
            .map(WeekStats::volume)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long loggedSessionCount = logged.stream().map(LoggedSession::sessionId).distinct().count();

    return new WeekStats(
        week.id(),
        normalised(planned),
        normalised(done),
        adherence(loggedSessionCount, sessionIds.size()));
  }

  private static BigDecimal volume(Prescription prescription) {
    return prescription
        .bellKg()
        .multiply(BigDecimal.valueOf((long) prescription.sets() * prescription.reps()));
  }

  private static BigDecimal volume(LoggedSet set) {
    return set.bellKg().multiply(BigDecimal.valueOf(set.reps()));
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
