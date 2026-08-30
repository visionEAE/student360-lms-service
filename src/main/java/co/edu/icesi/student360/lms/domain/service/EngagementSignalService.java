package co.edu.icesi.student360.lms.domain.service;

import co.edu.icesi.student360.lms.domain.model.AccessLog;
import co.edu.icesi.student360.lms.domain.model.ActivitySummary;
import co.edu.icesi.student360.lms.domain.model.Course;
import co.edu.icesi.student360.lms.domain.model.CourseActivity;
import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
import co.edu.icesi.student360.lms.domain.model.EngagementSignals;
import co.edu.icesi.student360.lms.domain.model.Participation;
import co.edu.icesi.student360.lms.domain.model.Submission;
import co.edu.icesi.student360.lms.domain.model.SubmissionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure computation of engagement signals from raw facts. No framework, no persistence: the
 * learning-domain interpretation lives here and nowhere else in the platform.
 */
public class EngagementSignalService {

  /** A course with no access in this many days counts as inactive. */
  public static final int ACTIVITY_WINDOW_DAYS = 30;

  /** Thresholds behind {@link Participation}, in days since the course was last opened. */
  private static final int LOW_THRESHOLD_DAYS = 14;

  private static final int MODERATE_THRESHOLD_DAYS = 7;
  private static final long INACTIVE_MISSING_THRESHOLD = 2;

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
        lastAccess.map(access -> daysSince(access.getOccurredAt(), now)).orElse(null);

    long onTime = count(submissions, SubmissionStatus.ON_TIME);
    long late = count(submissions, SubmissionStatus.LATE);
    long missing = count(submissions, SubmissionStatus.MISSING);
    BigDecimal onTimeRate = rate(onTime, late, missing);

    Instant windowStart = now.minus(Duration.ofDays(ACTIVITY_WINDOW_DAYS));
    List<AccessLog> withinWindow =
        recentAccesses.stream()
            .filter(access -> !access.getOccurredAt().isBefore(windowStart))
            .toList();
    Set<Integer> coursesWithActivity =
        withinWindow.stream().map(AccessLog::getCourseId).collect(Collectors.toSet());
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
        lastAccess.map(AccessLog::getOccurredAt).orElse(null),
        onTimeRate,
        coursesWithoutActivity,
        active.size(),
        withinWindow.size(),
        (int) late,
        (int) missing);
  }

  public ActivitySummary summarise(
      String studentId,
      int windowDays,
      List<CourseEnrollment> enrollments,
      List<Submission> submissions,
      List<AccessLog> accessesInWindow,
      Optional<AccessLog> lastAccess) {
    Instant now = clock.instant();
    Instant windowStart = now.minus(Duration.ofDays(windowDays));
    List<Submission> dueInWindow =
        submissions.stream()
            .filter(submission -> !submission.getAssignment().getDueAt().isBefore(windowStart))
            .toList();

    Map<Integer, Instant> lastAccessByCourse =
        accessesInWindow.stream()
            .collect(
                Collectors.toMap(
                    AccessLog::getCourseId,
                    AccessLog::getOccurredAt,
                    (a, b) -> a.isAfter(b) ? a : b));
    Map<Integer, List<Submission>> submissionsByCourse =
        dueInWindow.stream()
            .collect(Collectors.groupingBy(s -> s.getAssignment().getCourse().getId()));

    List<CourseActivity> courses =
        enrollments.stream()
            .filter(CourseEnrollment::isActive)
            .map(CourseEnrollment::getCourse)
            .sorted(Comparator.comparing(Course::getCode))
            .map(
                course -> {
                  Instant courseLastAccess = lastAccessByCourse.get(course.getId());
                  Integer days = courseLastAccess == null ? null : daysSince(courseLastAccess, now);
                  List<Submission> courseSubmissions =
                      submissionsByCourse.getOrDefault(course.getId(), List.of());
                  long onTime = count(courseSubmissions, SubmissionStatus.ON_TIME);
                  long late = count(courseSubmissions, SubmissionStatus.LATE);
                  long missing = count(courseSubmissions, SubmissionStatus.MISSING);
                  return new CourseActivity(
                      course.getCode(),
                      course.getName(),
                      courseLastAccess,
                      days,
                      onTime,
                      late,
                      missing,
                      classify(days, onTime, missing));
                })
            .toList();

    return new ActivitySummary(
        studentId,
        windowDays,
        accessesInWindow.size(),
        lastAccess.map(AccessLog::getOccurredAt).orElse(null),
        count(dueInWindow, SubmissionStatus.ON_TIME),
        count(dueInWindow, SubmissionStatus.LATE),
        count(dueInWindow, SubmissionStatus.MISSING),
        courses);
  }

  /**
   * A course reads INACTIVE when it has never been opened, or was last opened long ago with nothing
   * ever handed in on time; LOW when it has gone quiet for a while or work is piling up even though
   * the student still shows up sometimes; MODERATE when it has simply been a week or so; ACTIVE
   * otherwise. A single late submission does not, by itself, demote a course that is still being
   * visited regularly — the recency of access is what the label is about.
   */
  private static Participation classify(Integer daysSinceLastAccess, long onTime, long missing) {
    if (daysSinceLastAccess == null) {
      return Participation.INACTIVE;
    }
    if (daysSinceLastAccess > LOW_THRESHOLD_DAYS && onTime == 0) {
      return Participation.INACTIVE;
    }
    if (daysSinceLastAccess > LOW_THRESHOLD_DAYS || missing >= INACTIVE_MISSING_THRESHOLD) {
      return Participation.LOW;
    }
    if (daysSinceLastAccess > MODERATE_THRESHOLD_DAYS) {
      return Participation.MODERATE;
    }
    return Participation.ACTIVE;
  }

  private static BigDecimal rate(long onTime, long late, long missing) {
    long graded = onTime + late + missing;
    return graded == 0
        ? null
        : BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(graded), 2, RoundingMode.HALF_UP);
  }

  private static int daysSince(Instant moment, Instant now) {
    return (int) Duration.between(moment, now).toDays();
  }

  private static long count(List<Submission> submissions, SubmissionStatus status) {
    return submissions.stream().filter(submission -> submission.getStatus() == status).count();
  }
}
