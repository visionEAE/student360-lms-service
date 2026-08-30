package co.edu.icesi.student360.lms.api;

import co.edu.icesi.student360.lms.application.query.StudentSignalsQuery;
import co.edu.icesi.student360.lms.application.query.StudentSignalsQueryHandler;
import co.edu.icesi.student360.lms.application.query.model.EngagementSignalsModel;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Separate controller so the literal path never competes with {@code /students/{id}}. */
@RestController
@Validated
public class StudentSignalsController {

  private final StudentSignalsQueryHandler handler;

  public StudentSignalsController(StudentSignalsQueryHandler handler) {
    this.handler = handler;
  }

  @GetMapping("/api/lms/students/signals")
  public List<EngagementSignalsModel> signals(@RequestParam @NotEmpty List<String> ids) {
    return handler.handle(new StudentSignalsQuery(ids));
  }
}
