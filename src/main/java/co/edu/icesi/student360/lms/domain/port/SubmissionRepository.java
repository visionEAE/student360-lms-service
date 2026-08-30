package co.edu.icesi.student360.lms.domain.port;

import co.edu.icesi.student360.lms.domain.model.Submission;
import java.util.List;

public interface SubmissionRepository {

  List<Submission> findByStudentReference(String studentReference);
}
