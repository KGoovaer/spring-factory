package be.ae.springfactory.stats;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What a Program overview shows of one Session: its planned Volume (normalised like {@link
 * WeekStats}) and whether it has a Logged Session.
 */
public record SessionStats(UUID sessionId, BigDecimal plannedVolume, boolean logged) {}
