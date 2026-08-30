package co.edu.icesi.student360.lms.api;

import co.edu.icesi.student360.lms.application.query.FindActivityQuery;
import co.edu.icesi.student360.lms.application.query.FindActivityQueryHandler;
import co.edu.icesi.student360.lms.application.query.FindCoursesQuery;
import co.edu.icesi.student360.lms.application.query.FindCoursesQueryHandler;
import co.edu.icesi.student360.lms.application.query.FindSignalsQuery;
import co.edu.icesi.student360.lms.application.query.FindSignalsQueryHandler;
import co.edu.icesi.student360.lms.application.query.model.ActivityModel;
import co.edu.icesi.student360.lms.application.query.model.CourseModel;
import co.edu.icesi.student360.lms.application.query.model.EngagementSignalsModel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP ⇄ query translation only; the read models already have the contract's shape. */
@RestController
@Validated
@RequestMapping("/api/lms/students")
public class StudentEngagementController {

  private final FindCoursesQueryHandler findCourses;
  private final FindActivityQueryHandler findActivity;
  private final FindSignalsQueryHandler findSignals;

  public StudentEngagementController(
      FindCoursesQueryHandler findCourses,
      FindActivityQueryHandler findActivity,
      FindSignalsQueryHandler findSignals) {
    this.findCourses = findCourses;
    this.findActivity = findActivity;
    this.findSignals = findSignals;
  }

  @GetMapping("/{id}/courses")
  public List<CourseModel> courses(@PathVariable String id) {
    return findCourses.handle(new FindCoursesQuery(id));
  }

  @GetMapping("/{id}/activity")
  public ActivityModel activity(
      @PathVariable String id, @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
    return findActivity.handle(new FindActivityQuery(id, days));
  }

  @GetMapping("/{id}/signals")
  public EngagementSignalsModel signals(@PathVariable String id) {
    return findSignals.handle(new FindSignalsQuery(id));
  }
}
