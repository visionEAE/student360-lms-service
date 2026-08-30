package co.edu.icesi.student360.lms.application.query;

import co.edu.icesi.student360.common.api.exception.NotFoundException;
import co.edu.icesi.student360.common.audit.Audited;
import co.edu.icesi.student360.common.authorization.StudentRecordAccessPolicy;
import co.edu.icesi.student360.lms.application.query.model.CourseModel;
import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
import co.edu.icesi.student360.lms.domain.port.CourseEnrollmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of a student's learning activity. Same contract as core-service: authorization before
 * existence, every read audited with its basis. A student unknown to the LMS is one with no
 * enrollments.
 */
@Service
public class FindCoursesQueryHandler {

  static final String RESOURCE = "Student";

  private final CourseEnrollmentRepository enrollments;
  private final StudentRecordAccessPolicy accessPolicy;

  public FindCoursesQueryHandler(
      CourseEnrollmentRepository enrollments, StudentRecordAccessPolicy accessPolicy) {
    this.enrollments = enrollments;
    this.accessPolicy = accessPolicy;
  }

  @Audited(action = "READ_COURSES", subjectType = "STUDENT")
  @Transactional(readOnly = true)
  public List<CourseModel> handle(FindCoursesQuery query) {
    accessPolicy.assertCanRead(query.studentId());
    return requireEnrollments(query.studentId()).stream().map(CourseModel::from).toList();
  }

  List<CourseEnrollment> requireEnrollments(String studentId) {
    List<CourseEnrollment> enrolled = enrollments.findByStudentReference(studentId);
    if (enrolled.isEmpty()) {
      throw new NotFoundException(RESOURCE, studentId);
    }
    return enrolled;
  }
}
