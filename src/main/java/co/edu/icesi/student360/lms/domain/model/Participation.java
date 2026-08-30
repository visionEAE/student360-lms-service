package co.edu.icesi.student360.lms.domain.model;

/**
 * How engaged a student looks in one course. Computed by {@code EngagementSignalService}, never
 * stored: it is always a fresh read of the underlying facts.
 */
public enum Participation {
  ACTIVE,
  MODERATE,
  LOW,
  INACTIVE
}
