-- Seed relative to now() so the patterns stay meaningful whenever the demo runs.
-- S-1001 engaged: daily access in every course, everything on time.
-- S-1002 in between: last access five days ago, a few late submissions.
-- S-1003 disengaged (the at-risk student core-service also seeds): last access 21 days ago, only
-- one course touched in the last 30 days, mostly late or missing work.

INSERT INTO lms.course (code, name, term) VALUES
    ('ISI-301', 'Software Architecture', '2026-2'),
    ('MAT-201', 'Calculus II',           '2026-2'),
    ('HUM-110', 'Critical Thinking',     '2026-2');

INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT s.ref, c.id, 'ACTIVE'
FROM (VALUES ('S-1001'), ('S-1002'), ('S-1003')) AS s (ref)
CROSS JOIN lms.course c;

-- Three past-due assignments per course (25, 15 and 5 days ago) plus one still open.
INSERT INTO lms.assignment (course_id, title, type, due_at)
SELECT c.id, c.code || ' ' || a.title, a.type, now() - a.age
FROM lms.course c
CROSS JOIN (VALUES
    ('homework 1', 'HOMEWORK', interval '25 days'),
    ('quiz 1',     'QUIZ',     interval '15 days'),
    ('homework 2', 'HOMEWORK', interval '5 days'),
    ('project',    'PROJECT',  interval '-10 days')) AS a (title, type, age);

-- S-1001: every past-due assignment submitted the day before it was due.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1001', a.due_at - interval '1 day', 'ON_TIME'
FROM lms.assignment a WHERE a.due_at < now();

-- S-1002: on time except the quizzes, handed in two days late.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1002',
       CASE WHEN a.type = 'QUIZ' THEN a.due_at + interval '2 days' ELSE a.due_at - interval '1 day' END,
       CASE WHEN a.type = 'QUIZ' THEN 'LATE' ELSE 'ON_TIME' END
FROM lms.assignment a WHERE a.due_at < now();

-- S-1003: only the first homework of two courses on time, three late, the rest missing.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1003',
       CASE
           WHEN a.title LIKE '%homework 1' AND c.code <> 'HUM-110' THEN a.due_at - interval '1 day'
           WHEN a.title LIKE '%quiz 1' THEN a.due_at + interval '3 days'
           ELSE NULL
       END,
       CASE
           WHEN a.title LIKE '%homework 1' AND c.code <> 'HUM-110' THEN 'ON_TIME'
           WHEN a.title LIKE '%quiz 1' THEN 'LATE'
           ELSE 'MISSING'
       END
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id WHERE a.due_at < now();

-- Access logs. S-1001: every course, every day for the last week.
INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1001', c.id, now() - (d.day * interval '1 day') - interval '3 hours',
       CASE WHEN d.day % 3 = 0 THEN 'FORUM_POST' ELSE 'CONTENT_VIEW' END
FROM lms.course c CROSS JOIN generate_series(1, 7) AS d (day);

-- S-1002: two courses touched in the last two weeks, last time five days ago.
INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1002', c.id, now() - (d.day * interval '1 day') - interval '5 hours', 'CONTENT_VIEW'
FROM lms.course c CROSS JOIN (VALUES (5), (8), (12)) AS d (day)
WHERE c.code IN ('ISI-301', 'MAT-201');

-- S-1003: one course three weeks ago; the others last seen forty days ago, outside the window.
INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1003', c.id, now() - interval '21 days', 'CONTENT_VIEW' FROM lms.course c WHERE c.code = 'ISI-301'
UNION ALL
SELECT 'S-1003', c.id, now() - interval '40 days', 'LOGIN' FROM lms.course c WHERE c.code <> 'ISI-301';
