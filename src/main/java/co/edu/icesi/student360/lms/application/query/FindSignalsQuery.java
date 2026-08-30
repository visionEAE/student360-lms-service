package co.edu.icesi.student360.lms.application.query;

/** Read one student's interpreted engagement signals. */
public record FindSignalsQuery(String studentId) {

  /** The audit aspect records the first argument's string form as the subject id. */
  @Override
  public String toString() {
    return studentId;
  }
}
