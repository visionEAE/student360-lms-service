package co.edu.icesi.student360.lms.application.query.model;

import co.edu.icesi.student360.lms.domain.model.ActivitySummary;
import co.edu.icesi.student360.lms.domain.model.CourseActivity;
import java.time.Instant;
import java.util.List;

/** Contract v2 {@code EngagementActivity}. */
public record ActivityModel(
    String studentId,
    int windowDays,
    long accessCount,
    Instant lastAccessAt,
    SubmissionsModel submissions,
    List<CourseActivityModel> courses) {

  public record SubmissionsModel(long onTime, long late, long missing) {}

  public record CourseActivityModel(
      String courseCode,
      String courseName,
      Instant lastAccessAt,
      Integer daysSinceLastAccess,
      long onTime,
      long late,
      long missing,
      String participation) {

    static CourseActivityModel from(CourseActivity activity) {
      return new CourseActivityModel(
          activity.courseCode(),
          activity.courseName(),
          activity.lastAccessAt(),
          activity.daysSinceLastAccess(),
          activity.onTime(),
          activity.late(),
          activity.missing(),
          activity.participation().name());
    }
  }

  public static ActivityModel from(ActivitySummary summary) {
    return new ActivityModel(
        summary.studentId(),
        summary.windowDays(),
        summary.accessCount(),
        summary.lastAccessAt(),
        new SubmissionsModel(summary.onTime(), summary.late(), summary.missing()),
        summary.courses().stream().map(CourseActivityModel::from).toList());
  }
}
