package co.edu.icesi.student360.lms.infrastructure.config;

import co.edu.icesi.student360.common.authorization.StudentRecordAccessPolicy;
import co.edu.icesi.student360.lms.domain.port.AccessLogRepository;
import co.edu.icesi.student360.lms.domain.port.CourseEnrollmentRepository;
import co.edu.icesi.student360.lms.domain.port.SubmissionRepository;
import co.edu.icesi.student360.lms.domain.service.EngagementSignalService;
import co.edu.icesi.student360.lms.domain.service.StudentEngagementService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the domain services to their ports; the domain package carries no Spring stereotype. */
@Configuration
public class LmsConfiguration {

  @Bean
  public EngagementSignalService engagementSignalService(Clock clock) {
    return new EngagementSignalService(clock);
  }

  @Bean
  public StudentEngagementService studentEngagementService(
      CourseEnrollmentRepository enrollments,
      SubmissionRepository submissions,
      AccessLogRepository accesses,
      EngagementSignalService signals,
      StudentRecordAccessPolicy accessPolicy,
      Clock clock) {
    return new StudentEngagementService(
        enrollments, submissions, accesses, signals, accessPolicy, clock);
  }
}
