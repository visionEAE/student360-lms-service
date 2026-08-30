package co.edu.icesi.student360.lms.api.dto;

import co.edu.icesi.student360.lms.domain.model.EngagementSignals;
import java.math.BigDecimal;
import java.time.Instant;

/** The contract support-service consumes. Field names and semantics are fixed. */
public record EngagementSignalsResponse(
    String studentId,
    Instant computedAt,
    Integer daysSinceLastAccess,
    BigDecimal onTimeSubmissionRate,
    int coursesWithoutActivity,
    int activeCourses,
    int lateSubmissions,
    int missingSubmissions) {

  public static EngagementSignalsResponse from(EngagementSignals signals) {
    return new EngagementSignalsResponse(
        signals.studentId(),
        signals.computedAt(),
        signals.daysSinceLastAccess(),
        signals.onTimeSubmissionRate(),
        signals.coursesWithoutActivity(),
        signals.activeCourses(),
        signals.lateSubmissions(),
        signals.missingSubmissions());
  }
}
