package co.edu.icesi.student360.lms.application.query.model;

import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;

public record CourseModel(String courseCode, String courseName, String term, String status) {

  public static CourseModel from(CourseEnrollment enrollment) {
    return new CourseModel(
        enrollment.getCourse().getCode(),
        enrollment.getCourse().getName(),
        enrollment.getCourse().getTerm(),
        enrollment.getStatus().name());
  }
}
