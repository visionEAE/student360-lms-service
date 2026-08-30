package co.edu.icesi.student360.lms.infrastructure.persistence;

import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;
import co.edu.icesi.student360.lms.domain.port.CourseEnrollmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseEnrollmentJpaRepository
    extends JpaRepository<CourseEnrollment, Integer>, CourseEnrollmentRepository {}
