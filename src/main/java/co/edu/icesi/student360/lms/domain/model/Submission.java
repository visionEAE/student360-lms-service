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
import java.time.Instant;

/** One student's outcome for one past-due assignment; MISSING is stored, never inferred. */
@Entity
@Table(name = "submission", schema = "lms")
public class Submission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "assignment_id", nullable = false)
  private Assignment assignment;

  @Column(name = "student_reference", nullable = false)
  private String studentReference;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubmissionStatus status;

  protected Submission() {}

  public Assignment getAssignment() {
    return assignment;
  }

  public String getStudentReference() {
    return studentReference;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public SubmissionStatus getStatus() {
    return status;
  }
}
