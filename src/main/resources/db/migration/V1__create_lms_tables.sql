-- lms schema: the simulated learning platform. Owned by lms_user, migrated only by lms-service.
-- Schema-qualified on purpose. student_reference is the cross-service key (S-1001, ...) issued by
-- core-service and carried in the ref claim; the LMS never joins to core's tables.

CREATE TABLE lms.course (
    id    SERIAL PRIMARY KEY,
    code  TEXT NOT NULL,
    name  TEXT NOT NULL,
    term  TEXT NOT NULL,
    CONSTRAINT uq_course_code_term UNIQUE (code, term)
);

CREATE TABLE lms.course_enrollment (
    id                 SERIAL PRIMARY KEY,
    student_reference  TEXT NOT NULL,
    course_id          INT  NOT NULL REFERENCES lms.course (id),
    status             TEXT NOT NULL,                  -- ACTIVE | DROPPED
    CONSTRAINT uq_course_enrollment UNIQUE (student_reference, course_id),
    CONSTRAINT chk_course_enrollment_status CHECK (status IN ('ACTIVE', 'DROPPED'))
);

CREATE INDEX idx_course_enrollment_student ON lms.course_enrollment (student_reference);
CREATE INDEX idx_course_enrollment_course  ON lms.course_enrollment (course_id);

CREATE TABLE lms.assignment (
    id         SERIAL PRIMARY KEY,
    course_id  INT         NOT NULL REFERENCES lms.course (id),
    title      TEXT        NOT NULL,
    type       TEXT        NOT NULL,                   -- HOMEWORK | QUIZ | PROJECT | EXAM
    due_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_assignment_type CHECK (type IN ('HOMEWORK', 'QUIZ', 'PROJECT', 'EXAM'))
);

CREATE INDEX idx_assignment_course ON lms.assignment (course_id);
CREATE INDEX idx_assignment_due_at ON lms.assignment (due_at);

-- One row per student and past-due assignment; MISSING rows exist so absence is a fact, not an
-- inference.
CREATE TABLE lms.submission (
    id                 SERIAL PRIMARY KEY,
    assignment_id      INT         NOT NULL REFERENCES lms.assignment (id),
    student_reference  TEXT        NOT NULL,
    submitted_at       TIMESTAMPTZ,                    -- null when MISSING
    status             TEXT        NOT NULL,           -- ON_TIME | LATE | MISSING
    CONSTRAINT uq_submission UNIQUE (assignment_id, student_reference),
    CONSTRAINT chk_submission_status CHECK (status IN ('ON_TIME', 'LATE', 'MISSING')),
    CONSTRAINT chk_submission_missing CHECK ((status = 'MISSING') = (submitted_at IS NULL))
);

CREATE INDEX idx_submission_student ON lms.submission (student_reference);
CREATE INDEX idx_submission_assignment ON lms.submission (assignment_id);

-- Behavioural data: high frequency, append-only. This is the signal the support rule reads.
CREATE TABLE lms.access_log (
    id                 BIGSERIAL PRIMARY KEY,
    student_reference  TEXT        NOT NULL,
    course_id          INT         NOT NULL REFERENCES lms.course (id),
    occurred_at        TIMESTAMPTZ NOT NULL,
    access_type        TEXT        NOT NULL,           -- LOGIN | CONTENT_VIEW | FORUM_POST | SUBMISSION
    CONSTRAINT chk_access_log_type CHECK (access_type IN ('LOGIN', 'CONTENT_VIEW', 'FORUM_POST', 'SUBMISSION'))
);

CREATE INDEX idx_access_log_student_time ON lms.access_log (student_reference, occurred_at DESC);
CREATE INDEX idx_access_log_course       ON lms.access_log (course_id);
