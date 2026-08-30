package co.edu.icesi.student360.lms.domain.model;

import java.time.Instant;

/** One course's row in the per-course activity breakdown. */
public record CourseActivity(
    String courseCode,
    String courseName,
    Instant lastAccessAt,
    Integer daysSinceLastAccess,
    long onTime,
    long late,
    long missing,
    Participation participation) {}
