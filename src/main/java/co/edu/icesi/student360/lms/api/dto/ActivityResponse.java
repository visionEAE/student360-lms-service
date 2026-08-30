package co.edu.icesi.student360.lms.api.dto;

import co.edu.icesi.student360.lms.domain.model.ActivitySummary;
import java.time.Instant;

public record ActivityResponse(
    String studentId,
    int windowDays,
    long accessCount,
    Instant lastAccessAt,
    SubmissionsResponse submissions) {

  public record SubmissionsResponse(long onTime, long late, long missing) {}

  public static ActivityResponse from(ActivitySummary summary) {
    return new ActivityResponse(
        summary.studentId(),
        summary.windowDays(),
        summary.accessCount(),
        summary.lastAccessAt(),
        new SubmissionsResponse(summary.onTime(), summary.late(), summary.missing()));
  }
}
