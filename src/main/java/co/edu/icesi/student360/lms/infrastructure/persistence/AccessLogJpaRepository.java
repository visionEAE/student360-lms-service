package co.edu.icesi.student360.lms.infrastructure.persistence;

import co.edu.icesi.student360.lms.domain.model.AccessLog;
import co.edu.icesi.student360.lms.domain.port.AccessLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessLogJpaRepository
    extends JpaRepository<AccessLog, Long>, AccessLogRepository {}
