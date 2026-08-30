package co.edu.icesi.student360.lms.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One interaction with a course. Stored as the raw fact; the interpretation is computed. */
@Entity
@Table(name = "access_log", schema = "lms")
public class AccessLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "student_reference", nullable = false)
  private String studentReference;

  @Column(name = "course_id", nullable = false)
  private Integer courseId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "access_type", nullable = false)
  private String accessType;

  protected AccessLog() {}

  public String getStudentReference() {
    return studentReference;
  }

  public Integer getCourseId() {
    return courseId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getAccessType() {
    return accessType;
  }
}
