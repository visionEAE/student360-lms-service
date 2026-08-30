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
 * Phase gate 4: the at-risk student's signals are clearly distinguishable from the engaged
 * student's; same two-layer authorization and auditing contract as core-service.
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
        .andExpect(jsonPath("$.onTimeSubmissionRate").value(1.0))
        .andExpect(jsonPath("$.coursesWithoutActivity").value(0))
        .andExpect(jsonPath("$.activeCourses").value(3))
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
        .andExpect(jsonPath("$.daysSinceLastAccess", Matchers.greaterThanOrEqualTo(20)))
        .andExpect(jsonPath("$.onTimeSubmissionRate", Matchers.lessThan(0.5)))
        .andExpect(jsonPath("$.coursesWithoutActivity", Matchers.greaterThanOrEqualTo(2)))
        .andExpect(jsonPath("$.lateSubmissions").value(3))
        .andExpect(jsonPath("$.missingSubmissions").value(4));

    assertThat(single()).containsEntry("authorization_basis", "STAFF_ROLE");
  }

  @Test
  void shouldListCoursesAndSummariseActivity() throws Exception {
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
                "gate4-activity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.windowDays").value(20))
        .andExpect(jsonPath("$.accessCount").value(6))
        .andExpect(jsonPath("$.lastAccessAt").isNotEmpty())
        .andExpect(jsonPath("$.submissions.late").value(3))
        .andExpect(jsonPath("$.submissions.onTime").value(3));

    assertThat(jdbc.queryForList("SELECT action FROM audit.audit_record ORDER BY id", String.class))
        .containsExactly("READ_COURSES", "READ_ACTIVITY");
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
