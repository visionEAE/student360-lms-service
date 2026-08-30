package co.edu.icesi.student360.lms.domain.model;

import java.time.Instant;

/** Raw activity counts inside a window of {@code windowDays} days ending now. */
public record ActivitySummary(
    String studentId,
    int windowDays,
    long accessCount,
    Instant lastAccessAt,
    long onTime,
    long late,
    long missing) {}
