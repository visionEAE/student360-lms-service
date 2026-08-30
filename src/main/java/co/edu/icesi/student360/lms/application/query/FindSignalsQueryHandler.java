package co.edu.icesi.student360.lms.application.query;

import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.common.authorization.StudentRecordAccessPolicy;
import co.edu.icesi.student360.lms.application.query.model.EngagementSignalsModel;
import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
import co.edu.icesi.student360.lms.domain.model.EngagementSignals;
import co.edu.icesi.student360.lms.domain.port.AccessLogRepository;
import co.edu.icesi.student360.lms.domain.port.SubmissionRepository;
import co.edu.icesi.student360.lms.domain.service.EngagementSignalService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindSignalsQueryHandler {

  private final FindCoursesQueryHandler courses;
  private final SubmissionRepository submissions;
  private final AccessLogRepository accesses;
  private final EngagementSignalService signals;
  private final StudentRecordAccessPolicy accessPolicy;
  private final Clock clock;

  public FindSignalsQueryHandler(
      FindCoursesQueryHandler courses,
      SubmissionRepository submissions,
      AccessLogRepository accesses,
      EngagementSignalService signals,
      StudentRecordAccessPolicy accessPolicy,
      Clock clock) {
    this.courses = courses;
    this.submissions = submissions;
    this.accesses = accesses;
    this.signals = signals;
    this.accessPolicy = accessPolicy;
    this.clock = clock;
  }

  @Audited(action = "READ_ENGAGEMENT_SIGNALS", subjectType = "STUDENT")
  @Transactional(readOnly = true)
  public EngagementSignalsModel handle(FindSignalsQuery query) {
    accessPolicy.assertCanRead(query.studentId());
    return EngagementSignalsModel.from(computeSignals(query.studentId()));
  }

  EngagementSignals computeSignals(String studentId) {
    List<CourseEnrollment> enrolled = courses.requireEnrollments(studentId);
    Instant windowStart =
        clock.instant().minus(Duration.ofDays(EngagementSignalService.ACTIVITY_WINDOW_DAYS));
    return signals.compute(
        studentId,
        enrolled,
        submissions.findByStudentReference(studentId),
        accesses.findByStudentReferenceAndOccurredAtAfter(studentId, windowStart),
        accesses.findTopByStudentReferenceOrderByOccurredAtDesc(studentId));
  }
}
