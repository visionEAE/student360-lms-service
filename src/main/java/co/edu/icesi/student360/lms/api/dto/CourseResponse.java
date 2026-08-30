package co.edu.icesi.student360.lms.api.dto;

import co.edu.icesi.student360.lms.domain.model.CourseEnrollment;

public record CourseResponse(String courseCode, String courseName, String term, String status) {

  public static CourseResponse from(CourseEnrollment enrollment) {
    return new CourseResponse(
        enrollment.getCourse().getCode(),
        enrollment.getCourse().getName(),
        enrollment.getCourse().getTerm(),
        enrollment.getStatus().name());
  }
}
