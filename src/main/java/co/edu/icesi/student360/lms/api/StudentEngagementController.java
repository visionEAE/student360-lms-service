package co.edu.icesi.student360.lms.api;

import co.edu.icesi.student360.lms.api.dto.ActivityResponse;
import co.edu.icesi.student360.lms.api.dto.CourseResponse;
import co.edu.icesi.student360.lms.api.dto.EngagementSignalsResponse;
import co.edu.icesi.student360.lms.domain.service.StudentEngagementService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/lms/students")
public class StudentEngagementController {

  private final StudentEngagementService engagement;

  public StudentEngagementController(StudentEngagementService engagement) {
    this.engagement = engagement;
  }

  @GetMapping("/{id}/courses")
  public List<CourseResponse> courses(@PathVariable String id) {
    return engagement.findCourses(id).stream().map(CourseResponse::from).toList();
  }

  @GetMapping("/{id}/activity")
  public ActivityResponse activity(
      @PathVariable String id, @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
    return ActivityResponse.from(engagement.findActivity(id, days));
  }

  @GetMapping("/{id}/signals")
  public EngagementSignalsResponse signals(@PathVariable String id) {
    return EngagementSignalsResponse.from(engagement.findSignals(id));
  }
}
