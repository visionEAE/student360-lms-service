package co.edu.icesi.student360.lms.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.icesi.student360.lms.domain.model.AccessLog;
import co.edu.icesi.student360.lms.domain.model.Assignment;
import co.edu.icesi.student360.lms.domain.model.Course;
import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
import co.edu.icesi.student360.lms.domain.model.EngagementSignals;
import co.edu.icesi.student360.lms.domain.model.EnrollmentStatus;
import co.edu.icesi.student360.lms.domain.model.Submission;
import co.edu.icesi.student360.lms.domain.model.SubmissionStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** Pure logic, in-memory facts, fixed clock: no Spring context, no database. */
class EngagementSignalServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");
  private final EngagementSignalService service =
      new EngagementSignalService(Clock.fixed(NOW, ZoneOffset.UTC));
  private final Course architecture = course(1, "ISI-301");
  private final Course calculus = course(2, "MAT-201");
  private final Course thinking = course(3, "HUM-110");

  @Test
  void shouldFlagDisengagedStudent() {
    List<CourseEnrollment> enrollments =
        List.of(enrollment(architecture), enrollment(calculus), enrollment(thinking));
    List<Submission> submissions =
        List.of(
            submission(SubmissionStatus.ON_TIME),
            submission(SubmissionStatus.LATE),
            submission(SubmissionStatus.LATE),
            submission(SubmissionStatus.MISSING),
            submission(SubmissionStatus.MISSING));
    AccessLog last = access(architecture, NOW.minus(Duration.ofDays(21)));

    EngagementSignals signals =
        service.compute("S-1003", enrollments, submissions, List.of(last), Optional.of(last));

    assertThat(signals.daysSinceLastAccess()).isEqualTo(21);
    assertThat(signals.onTimeSubmissionRate()).isEqualByComparingTo(new BigDecimal("0.20"));
    assertThat(signals.coursesWithoutActivity()).isEqualTo(2);
    assertThat(signals.activeCourses()).isEqualTo(3);
    assertThat(signals.lateSubmissions()).isEqualTo(2);
    assertThat(signals.missingSubmissions()).isEqualTo(2);
    assertThat(signals.computedAt()).isEqualTo(NOW);
  }

  @Test
  void shouldReportEngagedStudentWithFullRateAndNoIdleCourses() {
    List<CourseEnrollment> enrollments = List.of(enrollment(architecture), enrollment(calculus));
    List<AccessLog> recent =
        List.of(
            access(architecture, NOW.minus(Duration.ofDays(1))),
            access(calculus, NOW.minus(Duration.ofHours(30))));

    EngagementSignals signals =
        service.compute(
            "S-1001",
            enrollments,
            List.of(submission(SubmissionStatus.ON_TIME), submission(SubmissionStatus.ON_TIME)),
            recent,
            Optional.of(recent.get(0)));

    assertThat(signals.daysSinceLastAccess()).isEqualTo(1);
    assertThat(signals.onTimeSubmissionRate()).isEqualByComparingTo(BigDecimal.ONE);
    assertThat(signals.coursesWithoutActivity()).isZero();
  }

  @Test
  void shouldLeaveSignalsNullWhenThereAreNoFacts() {
    EngagementSignals signals =
        service.compute(
            "S-9000", List.of(enrollment(calculus)), List.of(), List.of(), Optional.empty());

    assertThat(signals.daysSinceLastAccess()).isNull();
    assertThat(signals.onTimeSubmissionRate()).isNull();
    assertThat(signals.coursesWithoutActivity()).isEqualTo(1);
  }

  @Test
  void shouldIgnoreDroppedCoursesAndAccessesOutsideTheWindow() {
    CourseEnrollment dropped = enrollment(thinking);
    ReflectionTestUtils.setField(dropped, "status", EnrollmentStatus.DROPPED);
    AccessLog stale = access(calculus, NOW.minus(Duration.ofDays(40)));

    EngagementSignals signals =
        service.compute(
            "S-1002",
            List.of(enrollment(architecture), enrollment(calculus), dropped),
            List.of(),
            List.of(stale),
            Optional.of(stale));

    assertThat(signals.activeCourses()).isEqualTo(2);
    assertThat(signals.coursesWithoutActivity()).isEqualTo(2);
    assertThat(signals.daysSinceLastAccess()).isEqualTo(40);
  }

  private static Course course(int id, String code) {
    return instance(Course.class, Map.of("id", id, "code", code, "name", code, "term", "2026-2"));
  }

  private static CourseEnrollment enrollment(Course course) {
    return instance(
        CourseEnrollment.class,
        Map.of("studentReference", "S", "course", course, "status", EnrollmentStatus.ACTIVE));
  }

  private static Submission submission(SubmissionStatus status) {
    Assignment assignment =
        instance(
            Assignment.class,
            Map.of("title", "t", "type", "HOMEWORK", "dueAt", NOW.minus(Duration.ofDays(5))));
    return instance(
        Submission.class,
        Map.of("assignment", assignment, "studentReference", "S", "status", status));
  }

  private static AccessLog access(Course course, Instant at) {
    return instance(
        AccessLog.class,
        Map.of(
            "studentReference",
            "S",
            "courseId",
            course.getId(),
            "occurredAt",
            at,
            "accessType",
            "CONTENT_VIEW"));
  }

  /** Entities are built by JPA in production; here through their protected constructors. */
  private static <T> T instance(Class<T> type, Map<String, Object> fields) {
    T entity = BeanUtils.instantiateClass(type);
    fields.forEach((name, value) -> ReflectionTestUtils.setField(entity, name, value));
    return entity;
  }
}
