package co.edu.icesi.student360.lms.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The LMS's own interpretation of a student's behaviour. Exposed already computed so that
 * support-service consumes a signal and never re-derives learning-domain knowledge.
 */
public record EngagementSignals(
    String studentId,
    Instant computedAt,
    Integer daysSinceLastAccess,
    Instant lastAccessAt,
    BigDecimal onTimeSubmissionRate,
    int coursesWithoutActivity,
    int activeCourses,
    int accessCount30d,
    int lateSubmissions,
    int missingSubmissions) {}
