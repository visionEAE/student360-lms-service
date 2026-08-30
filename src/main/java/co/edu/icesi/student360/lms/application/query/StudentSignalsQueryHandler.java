package co.edu.icesi.student360.lms.application.query;

import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.lms.application.query.model.EngagementSignalsModel;
import co.edu.icesi.student360.lms.domain.policy.StaffAccessPolicy;
import co.edu.icesi.student360.lms.domain.port.CourseEnrollmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Staff only. A student unknown to the LMS (no enrollments) is skipped, not an error. */
@Service
public class StudentSignalsQueryHandler {

  static final String SUBJECT_TYPE = "STUDENT_BATCH";

  private final CourseEnrollmentRepository enrollments;
  private final FindSignalsQueryHandler findSignals;
  private final StaffAccessPolicy staffPolicy;

  public StudentSignalsQueryHandler(
      CourseEnrollmentRepository enrollments,
      FindSignalsQueryHandler findSignals,
      StaffAccessPolicy staffPolicy) {
    this.enrollments = enrollments;
    this.findSignals = findSignals;
    this.staffPolicy = staffPolicy;
  }

  @Audited(action = "READ_ENGAGEMENT_SIGNALS_BATCH", subjectType = SUBJECT_TYPE)
  @Transactional(readOnly = true)
  public List<EngagementSignalsModel> handle(StudentSignalsQuery query) {
    staffPolicy.assertStaff(SUBJECT_TYPE, query.toString());
    return query.studentIds().stream()
        .filter(id -> !enrollments.findByStudentReference(id).isEmpty())
        .map(id -> EngagementSignalsModel.from(findSignals.computeSignals(id)))
        .toList();
  }
}
