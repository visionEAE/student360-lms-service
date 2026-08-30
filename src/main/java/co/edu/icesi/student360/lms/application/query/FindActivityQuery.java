package co.edu.icesi.student360.lms.application.query;

/** Read one student's raw activity over the last {@code windowDays} days, per course. */
public record FindActivityQuery(String studentId, int windowDays) {

  /** The audit aspect records the first argument's string form as the subject id. */
  @Override
  public String toString() {
    return studentId;
  }
}
