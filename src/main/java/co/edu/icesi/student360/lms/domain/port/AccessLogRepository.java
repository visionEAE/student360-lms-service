package co.edu.icesi.student360.lms.domain.port;

import co.edu.icesi.student360.lms.domain.model.AccessLog;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccessLogRepository {

  List<AccessLog> findByStudentReferenceAndOccurredAtAfter(String studentReference, Instant since);

  Optional<AccessLog> findTopByStudentReferenceOrderByOccurredAtDesc(String studentReference);
}
