package co.edu.icesi.student360.lms.application.query;

import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.common.authorization.StudentRecordAccessPolicy;
import co.edu.icesi.student360.lms.application.query.model.ActivityModel;
import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
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
public class FindActivityQueryHandler {

  private final FindCoursesQueryHandler courses;
  private final SubmissionRepository submissions;
  private final AccessLogRepository accesses;
  private final EngagementSignalService signals;
  private final StudentRecordAccessPolicy accessPolicy;
  private final Clock clock;

  public FindActivityQueryHandler(
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

  @Audited(action = "READ_ACTIVITY", subjectType = "STUDENT")
  @Transactional(readOnly = true)
  public ActivityModel handle(FindActivityQuery query) {
    accessPolicy.assertCanRead(query.studentId());
    List<CourseEnrollment> enrolled = courses.requireEnrollments(query.studentId());
    Instant windowStart = clock.instant().minus(Duration.ofDays(query.windowDays()));
    return ActivityModel.from(
        signals.summarise(
            query.studentId(),
            query.windowDays(),
            enrolled,
            submissions.findByStudentReference(query.studentId()),
            accesses.findByStudentReferenceAndOccurredAtAfter(query.studentId(), windowStart),
            accesses.findTopByStudentReferenceOrderByOccurredAtDesc(query.studentId())));
  }
}
