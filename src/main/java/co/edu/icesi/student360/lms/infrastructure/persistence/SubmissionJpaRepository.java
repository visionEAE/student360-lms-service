package co.edu.icesi.student360.lms.infrastructure.persistence;

import co.edu.icesi.student360.lms.domain.model.Submission;
import co.edu.icesi.student360.lms.domain.port.SubmissionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionJpaRepository
    extends JpaRepository<Submission, Integer>, SubmissionRepository {}
