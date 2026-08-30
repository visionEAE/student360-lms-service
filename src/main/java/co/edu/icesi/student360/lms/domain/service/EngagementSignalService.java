package co.edu.icesi.student360.lms.domain.service;

import co.edu.icesi.student360.lms.domain.model.AccessLog;
import co.edu.icesi.student360.lms.domain.model.ActivitySummary;
import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
import co.edu.icesi.student360.lms.domain.model.EngagementSignals;
import co.edu.icesi.student360.lms.domain.model.Submission;
import co.edu.icesi.student360.lms.domain.model.SubmissionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure computation of engagement signals from raw facts. No framework, no persistence: the
 * learning-domain interpretation lives here and nowhere else in the platform.
 */
public class EngagementSignalService {

  /** A course with no access in this many days counts as inactive. */
  static final int ACTIVITY_WINDOW_DAYS = 30;

  private final Clock clock;

  public EngagementSignalService(Clock clock) {
    this.clock = clock;
  }

  public EngagementSignals compute(
      String studentId,
      List<CourseEnrollment> enrollments,
      List<Submission> submissions,
      List<AccessLog> recentAccesses,
      Optional<AccessLog> lastAccess) {
    Instant now = clock.instant();
    Integer daysSinceLastAccess =
        lastAccess
            .map(access -> (int) Duration.between(access.getOccurredAt(), now).toDays())
            .orElse(null);

    long onTime = count(submissions, SubmissionStatus.ON_TIME);
    long late = count(submissions, SubmissionStatus.LATE);
    long missing = count(submissions, SubmissionStatus.MISSING);
    long graded = onTime + late + missing;
    BigDecimal onTimeRate =
        graded == 0
            ? null
            : BigDecimal.valueOf(onTime)
                .divide(BigDecimal.valueOf(graded), 2, RoundingMode.HALF_UP);

    Instant windowStart = now.minus(Duration.ofDays(ACTIVITY_WINDOW_DAYS));
    Set<Integer> coursesWithActivity =
        recentAccesses.stream()
            .filter(access -> !access.getOccurredAt().isBefore(windowStart))
            .map(AccessLog::getCourseId)
            .collect(Collectors.toSet());
    List<CourseEnrollment> active =
        enrollments.stream().filter(CourseEnrollment::isActive).toList();
    int coursesWithoutActivity =
        (int)
            active.stream()
                .filter(enrollment -> !coursesWithActivity.contains(enrollment.getCourse().getId()))
                .count();

    return new EngagementSignals(
        studentId,
        now,
        daysSinceLastAccess,
        onTimeRate,
        coursesWithoutActivity,
        active.size(),
        (int) late,
        (int) missing);
  }

  public ActivitySummary summarise(
      String studentId,
      int windowDays,
      List<Submission> submissions,
      List<AccessLog> accessesInWindow,
      Optional<AccessLog> lastAccess) {
    Instant windowStart = clock.instant().minus(Duration.ofDays(windowDays));
    List<Submission> dueInWindow =
        submissions.stream()
            .filter(submission -> !submission.getAssignment().getDueAt().isBefore(windowStart))
            .toList();
    return new ActivitySummary(
        studentId,
        windowDays,
        accessesInWindow.size(),
        lastAccess.map(AccessLog::getOccurredAt).orElse(null),
        count(dueInWindow, SubmissionStatus.ON_TIME),
        count(dueInWindow, SubmissionStatus.LATE),
        count(dueInWindow, SubmissionStatus.MISSING));
  }

  private static long count(List<Submission> submissions, SubmissionStatus status) {
    return submissions.stream().filter(submission -> submission.getStatus() == status).count();
  }
}
