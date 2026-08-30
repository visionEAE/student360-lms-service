package co.edu.icesi.student360.lms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.icesi.student360.common.identity.IdentityHeaders;
import co.edu.icesi.student360.common.logging.Correlation;
import co.edu.icesi.student360.common.security.ServiceTokenProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Phase gate 4 plus contract v2: the at-risk student's signals are clearly distinguishable from the
 * engaged student's; the per-course breakdown carries the design's participation labels; the batch
 * signals endpoint is a staff capability.
 */
@SpringBootTest(
    properties = {
      "LMS_DB_PASSWORD=unused-overridden-by-testcontainers",
      "SERVICE_TOKEN_SECRET=0123456789abcdef0123456789abcdef-test-only"
    })
@AutoConfigureMockMvc
@Testcontainers
class StudentEngagementIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16").withInitScript("db/test-init.sql");

  private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-000000001001");
  private static final UUID CARLOS = UUID.fromString("22222222-2222-2222-2222-000000002001");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ServiceTokenProvider tokens;

  @BeforeEach
  void cleanAuditTrail() {
    jdbc.update("DELETE FROM audit.audit_record");
  }

  @Test
  void shouldReturnOwnSignalsAndAuditWithSelfBasis() throws Exception {
    mockMvc
        .perform(
            as(ANA, "STUDENT", "S-1001", get("/api/lms/students/S-1001/signals"), "gate4-self"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentId").value("S-1001"))
        .andExpect(jsonPath("$.daysSinceLastAccess", Matchers.lessThanOrEqualTo(2)))
        .andExpect(jsonPath("$.lastAccessAt").isNotEmpty())
        .andExpect(jsonPath("$.onTimeSubmissionRate").value(1.0))
        .andExpect(jsonPath("$.coursesWithoutActivity").value(0))
        .andExpect(jsonPath("$.activeCourses").value(3))
        .andExpect(jsonPath("$.accessCount30d", Matchers.greaterThan(0)))
        .andExpect(jsonPath("$.lateSubmissions").value(0))
        .andExpect(jsonPath("$.missingSubmissions").value(0))
        .andExpect(jsonPath("$.computedAt").isNotEmpty());

    assertThat(single())
        .containsEntry("action", "READ_ENGAGEMENT_SIGNALS")
        .containsEntry("subject_type", "STUDENT")
        .containsEntry("subject_id", "S-1001")
        .containsEntry("authorization_basis", "SELF")
        .containsEntry("outcome", "ALLOWED")
        .containsEntry("actor_id", ANA)
        .containsEntry("request_id", "gate4-self")
        .containsEntry("service_name", "lms-service");
  }

  @Test
  void shouldDenyAnotherStudentsSignalsAndAuditTheDenial() throws Exception {
    mockMvc
        .perform(
            as(ANA, "STUDENT", "S-1001", get("/api/lms/students/S-1003/signals"), "gate4-denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Access denied"))
        .andExpect(jsonPath("$.requestId").value("gate4-denied"));

    assertThat(single())
        .containsEntry("subject_id", "S-1003")
        .containsEntry("authorization_basis", "NONE")
        .containsEntry("outcome", "DENIED")
        .containsEntry("actor_id", ANA);
  }

  @Test
  void shouldExposeTheDisengagedPatternOfTheAtRiskStudent() throws Exception {
    mockMvc
        .perform(
            as(CARLOS, "ADVISOR", "A-2001", get("/api/lms/students/S-1003/signals"), "gate4-risk"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentId").value("S-1003"))
        // The most recent of the five courses (3 days ago) is the honest overall answer; see the
        // seed migration's note on why this deviates from the design mock's own headline number.
        .andExpect(jsonPath("$.daysSinceLastAccess").value(3))
        .andExpect(jsonPath("$.onTimeSubmissionRate").value(0.52))
        .andExpect(jsonPath("$.coursesWithoutActivity").value(0))
        .andExpect(jsonPath("$.activeCourses").value(5))
        .andExpect(jsonPath("$.accessCount30d").value(6))
        .andExpect(jsonPath("$.lateSubmissions").value(5))
        .andExpect(jsonPath("$.missingSubmissions").value(7));

    assertThat(single()).containsEntry("authorization_basis", "STAFF_ROLE");
  }

  @Test
  void shouldBreakDownActivityPerCourseMatchingTheDesignParticipationLabels() throws Exception {
    mockMvc
        .perform(
            as(
                CARLOS,
                "ADVISOR",
                "A-2001",
                get("/api/lms/students/S-1003/activity?days=30"),
                "gate4-activity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentId").value("S-1003"))
        .andExpect(jsonPath("$.windowDays").value(30))
        .andExpect(jsonPath("$.accessCount").value(6))
        .andExpect(jsonPath("$.submissions.onTime").value(13))
        .andExpect(jsonPath("$.submissions.late").value(5))
        .andExpect(jsonPath("$.submissions.missing").value(7))
        .andExpect(jsonPath("$.courses.length()").value(5))
        .andExpect(jsonPath("$.courses[?(@.courseCode == 'PSI-301')].daysSinceLastAccess").value(3))
        .andExpect(
            jsonPath("$.courses[?(@.courseCode == 'PSI-301')].participation").value("ACTIVE"))
        .andExpect(
            jsonPath("$.courses[?(@.courseCode == 'PSI-310')].daysSinceLastAccess").value(18))
        .andExpect(jsonPath("$.courses[?(@.courseCode == 'PSI-310')].participation").value("LOW"))
        .andExpect(jsonPath("$.courses[?(@.courseCode == 'PSI-320')].daysSinceLastAccess").value(5))
        .andExpect(
            jsonPath("$.courses[?(@.courseCode == 'PSI-320')].participation").value("ACTIVE"))
        .andExpect(
            jsonPath("$.courses[?(@.courseCode == 'EST-201')].daysSinceLastAccess").value(20))
        .andExpect(
            jsonPath("$.courses[?(@.courseCode == 'EST-201')].participation").value("INACTIVE"))
        .andExpect(
            jsonPath("$.courses[?(@.courseCode == 'HUM-210')].daysSinceLastAccess").value(12))
        .andExpect(
            jsonPath("$.courses[?(@.courseCode == 'HUM-210')].participation").value("MODERATE"));

    assertThat(jdbc.queryForList("SELECT action FROM audit.audit_record", String.class))
        .containsExactly("READ_ACTIVITY");
  }

  @Test
  void shouldListCoursesAndSummariseActivityForAnUnaffectedStudent() throws Exception {
    mockMvc
        .perform(
            as(
                CARLOS,
                "ADVISOR",
                "A-2001",
                get("/api/lms/students/S-1002/courses"),
                "gate4-courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].courseCode").isNotEmpty())
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    mockMvc
        .perform(
            as(
                CARLOS,
                "ADVISOR",
                "A-2001",
                get("/api/lms/students/S-1002/activity?days=20"),
                "gate4-activity-1002"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.windowDays").value(20))
        .andExpect(jsonPath("$.accessCount").value(6))
        .andExpect(jsonPath("$.lastAccessAt").isNotEmpty())
        .andExpect(jsonPath("$.submissions.late").value(3))
        .andExpect(jsonPath("$.submissions.onTime").value(3))
        .andExpect(jsonPath("$.courses.length()").value(3));

    assertThat(jdbc.queryForList("SELECT action FROM audit.audit_record ORDER BY id", String.class))
        .containsExactly("READ_COURSES", "READ_ACTIVITY");
  }

  @Test
  void shouldExposeContrastingProfilesForTheOtherAdviseesSeeded() throws Exception {
    // S-1004: disengaged (no access in over three weeks, mostly missing work).
    mockMvc
        .perform(
            as(CARLOS, "ADVISOR", "A-2001", get("/api/lms/students/S-1004/signals"), "gate4-1004"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.daysSinceLastAccess", Matchers.greaterThanOrEqualTo(20)))
        .andExpect(jsonPath("$.missingSubmissions", Matchers.greaterThan(0)));

    // S-1006: fully engaged, everything on time.
    mockMvc
        .perform(
            as(CARLOS, "ADVISOR", "A-2001", get("/api/lms/students/S-1006/signals"), "gate4-1006"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.daysSinceLastAccess", Matchers.lessThanOrEqualTo(4)))
        .andExpect(jsonPath("$.onTimeSubmissionRate").value(1.0))
        .andExpect(jsonPath("$.missingSubmissions").value(0));
  }

  @Test
  void shouldLetStaffReadSignalsInBatchSkippingUnknownStudents() throws Exception {
    mockMvc
        .perform(
            as(
                CARLOS,
                "ADVISOR",
                "A-2001",
                get("/api/lms/students/signals").param("ids", "S-1003,S-1001,S-9999"),
                "v2-batch"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].studentId").value("S-1003"))
        .andExpect(jsonPath("$[0].coursesWithoutActivity").value(0))
        .andExpect(jsonPath("$[1].studentId").value("S-1001"));

    assertThat(single())
        .containsEntry("action", "READ_ENGAGEMENT_SIGNALS_BATCH")
        .containsEntry("subject_type", "STUDENT_BATCH")
        .containsEntry("subject_id", "S-1003,S-1001,S-9999")
        .containsEntry("authorization_basis", "STAFF_ROLE")
        .containsEntry("outcome", "ALLOWED");
  }

  @Test
  void shouldDenyBatchSignalsToStudentsAndRejectOversizedBatches() throws Exception {
    mockMvc
        .perform(
            as(
                ANA,
                "STUDENT",
                "S-1001",
                get("/api/lms/students/signals").param("ids", "S-1001,S-1003"),
                "v2-batch-denied"))
        .andExpect(status().isForbidden());
    assertThat(single())
        .containsEntry("action", "READ_ENGAGEMENT_SIGNALS_BATCH")
        .containsEntry("outcome", "DENIED")
        .containsEntry("authorization_basis", "NONE");

    String tooMany =
        IntStream.rangeClosed(1, 101).mapToObj(i -> "S-" + i).collect(Collectors.joining(","));
    mockMvc
        .perform(
            as(
                CARLOS,
                "ADVISOR",
                "A-2001",
                get("/api/lms/students/signals").param("ids", tooMany),
                "v2-batch-too-many"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid request"));
  }

  @Test
  void shouldHideExistenceFromUnauthorizedStudents() throws Exception {
    mockMvc
        .perform(
            as(ANA, "STUDENT", "S-1001", get("/api/lms/students/S-9999/signals"), "gate4-hidden"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            as(CARLOS, "ADVISOR", "A-2001", get("/api/lms/students/S-9999/signals"), "gate4-404"))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRejectCallsWithoutServiceTokenBeforeReachingTheDomain() throws Exception {
    mockMvc
        .perform(
            get("/api/lms/students/S-1001/signals")
                .header(IdentityHeaders.USER_ID, ANA.toString())
                .header(IdentityHeaders.USER_ROLES, "STUDENT")
                .header(IdentityHeaders.EXTERNAL_REFERENCE, "S-1001"))
        .andExpect(status().isUnauthorized());

    assertThat(jdbc.queryForList("SELECT * FROM audit.audit_record")).isEmpty();
  }

  private MockHttpServletRequestBuilder as(
      UUID userId,
      String role,
      String reference,
      MockHttpServletRequestBuilder request,
      String requestId) {
    return request
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.tokenFor("lms-service"))
        .header(Correlation.REQUEST_ID_HEADER, requestId)
        .header(IdentityHeaders.USER_ID, userId.toString())
        .header(IdentityHeaders.USER_ROLES, role)
        .header(IdentityHeaders.EXTERNAL_REFERENCE, reference);
  }

  private Map<String, Object> single() {
    List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM audit.audit_record");
    assertThat(rows).hasSize(1);
    return rows.get(0);
  }
}
