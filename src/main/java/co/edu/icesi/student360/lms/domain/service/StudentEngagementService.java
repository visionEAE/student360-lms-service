package co.edu.icesi.student360.lms.domain.service;

import co.edu.icesi.student360.common.api.exception.NotFoundException;
import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.common.authorization.StudentRecordAccessPolicy;
import co.edu.icesi.student360.lms.domain.model.ActivitySummary;
import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
import co.edu.icesi.student360.lms.domain.model.EngagementSignals;
import co.edu.icesi.student360.lms.domain.port.AccessLogRepository;
import co.edu.icesi.student360.lms.domain.port.CourseEnrollmentRepository;
import co.edu.icesi.student360.lms.domain.port.SubmissionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of a student's learning activity. Same contract as core-service: authorization before
 * existence, every read audited with its basis. A student unknown to the LMS is one with no
 * enrollments.
 */
public class StudentEngagementService {

  static final String RESOURCE = "Student";

  private final CourseEnrollmentRepository enrollments;
  private final SubmissionRepository submissions;
  private final AccessLogRepository accesses;
  private final EngagementSignalService signals;
  private final StudentRecordAccessPolicy accessPolicy;
  private final Clock clock;

  public StudentEngagementService(
      CourseEnrollmentRepository enrollments,
      SubmissionRepository submissions,
      AccessLogRepository accesses,
      EngagementSignalService signals,
      StudentRecordAccessPolicy accessPolicy,
      Clock clock) {
    this.enrollments = enrollments;
    this.submissions = submissions;
    this.accesses = accesses;
    this.signals = signals;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Audited(action = "READ_COURSES", subjectType = "STUDENT")
  @Transactional(readOnly = true)
  public List<CourseEnrollment> findCourses(String studentId) {
    accessPolicy.assertCanRead(studentId);
    return requireEnrollments(studentId);
  }

  @Audited(action = "READ_ACTIVITY", subjectType = "STUDENT")
  @Transactional(readOnly = true)
  public ActivitySummary findActivity(String studentId, int windowDays) {
    accessPolicy.assertCanRead(studentId);
    requireEnrollments(studentId);
    Instant windowStart = clock.instant().minus(Duration.ofDays(windowDays));
    return signals.summarise(
        studentId,
        windowDays,
        submissions.findByStudentReference(studentId),
        accesses.findByStudentReferenceAndOccurredAtAfter(studentId, windowStart),
        accesses.findTopByStudentReferenceOrderByOccurredAtDesc(studentId));
  }

  @Audited(action = "READ_ENGAGEMENT_SIGNALS", subjectType = "STUDENT")
  @Transactional(readOnly = true)
  public EngagementSignals findSignals(String studentId) {
    accessPolicy.assertCanRead(studentId);
    List<CourseEnrollment> enrolled = requireEnrollments(studentId);
    Instant windowStart =
        clock.instant().minus(Duration.ofDays(EngagementSignalService.ACTIVITY_WINDOW_DAYS));
    return signals.compute(
        studentId,
        enrolled,
        submissions.findByStudentReference(studentId),
        accesses.findByStudentReferenceAndOccurredAtAfter(studentId, windowStart),
        accesses.findTopByStudentReferenceOrderByOccurredAtDesc(studentId));
  }

  private List<CourseEnrollment> requireEnrollments(String studentId) {
    List<CourseEnrollment> enrolled = enrollments.findByStudentReference(studentId);
    if (enrolled.isEmpty()) {
      throw new NotFoundException(RESOURCE, studentId);
    }
    return enrolled;
  }
}
