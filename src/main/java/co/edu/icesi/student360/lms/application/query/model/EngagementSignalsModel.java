package co.edu.icesi.student360.lms.application.query.model;

import co.edu.icesi.student360.lms.domain.model.EngagementSignals;
import java.math.BigDecimal;
import java.time.Instant;

/** Contract v2 {@code EngagementSignals}. The field names and semantics are fixed. */
public record EngagementSignalsModel(
    String studentId,
    Instant computedAt,
    Integer daysSinceLastAccess,
    Instant lastAccessAt,
    BigDecimal onTimeSubmissionRate,
    int coursesWithoutActivity,
    int activeCourses,
    int accessCount30d,
    int lateSubmissions,
    int missingSubmissions) {

  public static EngagementSignalsModel from(EngagementSignals signals) {
    return new EngagementSignalsModel(
        signals.studentId(),
        signals.computedAt(),
        signals.daysSinceLastAccess(),
        signals.lastAccessAt(),
        signals.onTimeSubmissionRate(),
        signals.coursesWithoutActivity(),
        signals.activeCourses(),
        signals.accessCount30d(),
        signals.lateSubmissions(),
        signals.missingSubmissions());
  }
}
