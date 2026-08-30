package co.edu.icesi.student360.lms.domain.port;

import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
import java.util.List;

public interface CourseEnrollmentRepository {

  List<CourseEnrollment> findByStudentReference(String studentReference);
}
