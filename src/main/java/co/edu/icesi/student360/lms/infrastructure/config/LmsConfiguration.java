package co.edu.icesi.student360.lms.infrastructure.config;

import co.edu.icesi.student360.lms.domain.policy.StaffAccessPolicy;
import co.edu.icesi.student360.lms.domain.service.EngagementSignalService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the pure domain classes; the domain package carries no Spring stereotype. */
@Configuration
public class LmsConfiguration {

  @Bean
  public EngagementSignalService engagementSignalService(Clock clock) {
    return new EngagementSignalService(clock);
  }

  @Bean
  public StaffAccessPolicy staffAccessPolicy() {
    return new StaffAccessPolicy();
  }
}
