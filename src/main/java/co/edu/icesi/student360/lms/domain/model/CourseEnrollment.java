package co.edu.icesi.student360.lms.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "course_enrollment", schema = "lms")
public class CourseEnrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "student_reference", nullable = false)
  private String studentReference;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnrollmentStatus status;

  protected CourseEnrollment() {}

  public String getStudentReference() {
    return studentReference;
  }

  public Course getCourse() {
    return course;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public boolean isActive() {
    return status == EnrollmentStatus.ACTIVE;
  }
}
