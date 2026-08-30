-- Engagement for the four advisees added in core-service V5 (docs/api-contract-v2.md showcase).
-- Juan Pablo and Isabella are engaged; Santiago is moderately engaged (matches his academic
-- watch); Andrés is engaged (his risk is purely financial).

INSERT INTO lms.course (code, name, term) VALUES
    ('ISI-401', 'Cloud Computing',        '2026-2'),
    ('ISI-410', 'Ingeniería de Software', '2026-2'),
    ('DER-301', 'Derecho Penal',          '2026-2'),
    ('DER-310', 'Derecho Laboral',        '2026-2'),
    ('CIV-501', 'Estructuras Avanzadas',  '2026-2'),
    ('CIV-510', 'Geotecnia',              '2026-2'),
    ('ECO-501', 'Econometría',            '2026-2'),
    ('ECO-510', 'Finanzas Públicas',      '2026-2');

INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT 'S-1007', c.id, 'ACTIVE' FROM lms.course c WHERE c.code IN ('ISI-401', 'ISI-410', 'HUM-110') AND c.term = '2026-2';
INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT 'S-1008', c.id, 'ACTIVE' FROM lms.course c WHERE c.code IN ('DER-301', 'DER-310', 'HUM-110') AND c.term = '2026-2';
INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT 'S-1009', c.id, 'ACTIVE' FROM lms.course c WHERE c.code IN ('CIV-501', 'CIV-510') AND c.term = '2026-2';
INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT 'S-1010', c.id, 'ACTIVE' FROM lms.course c WHERE c.code IN ('ECO-501', 'ECO-510', 'HUM-210') AND c.term = '2026-2';

INSERT INTO lms.assignment (course_id, title, type, due_at)
SELECT c.id, c.code || ' ' || a.title, a.type, now() - a.age
FROM lms.course c
CROSS JOIN (VALUES
    ('homework 1', 'HOMEWORK', interval '20 days'),
    ('quiz 1',     'QUIZ',     interval '12 days'),
    ('homework 2', 'HOMEWORK', interval '5 days')) AS a (title, type, age)
WHERE c.code IN ('ISI-401', 'ISI-410', 'DER-301', 'DER-310', 'CIV-501', 'CIV-510', 'ECO-501', 'ECO-510')
  AND c.term = '2026-2';

-- Juan Pablo: everything on time, accessed 1 day ago.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1007', a.due_at - interval '1 day', 'ON_TIME'
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code IN ('ISI-401', 'ISI-410') AND a.due_at < now();

INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1007', c.id, now() - (d || ' days')::interval, 'CONTENT_VIEW'
FROM lms.course c CROSS JOIN generate_series(1, 6, 2) AS d
WHERE c.code IN ('ISI-401', 'ISI-410') AND c.term = '2026-2';

-- Santiago: one late, one missing per course, last access 9 days ago (moderate).
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1008',
       CASE a.title WHEN c.code || ' homework 1' THEN a.due_at - interval '1 day' ELSE a.due_at + interval '3 days' END,
       CASE a.title WHEN c.code || ' homework 1' THEN 'ON_TIME' ELSE 'LATE' END
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code IN ('DER-301', 'DER-310') AND a.title != c.code || ' homework 2' AND a.due_at < now();

INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1008', c.id, now() - (d || ' days')::interval, 'CONTENT_VIEW'
FROM lms.course c CROSS JOIN generate_series(9, 13, 4) AS d
WHERE c.code IN ('DER-301', 'DER-310') AND c.term = '2026-2';

-- Isabella: engaged and on time, accessed 2 days ago.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1009', a.due_at - interval '1 day', 'ON_TIME'
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code IN ('CIV-501', 'CIV-510') AND a.due_at < now();

INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1009', c.id, now() - (d || ' days')::interval, 'CONTENT_VIEW'
FROM lms.course c CROSS JOIN generate_series(2, 8, 3) AS d
WHERE c.code IN ('CIV-501', 'CIV-510') AND c.term = '2026-2';

-- Andrés: engaged, mostly on time, accessed 4 days ago — the design's "watch" is financial only.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1010',
       CASE a.title WHEN c.code || ' quiz 1' THEN a.due_at + interval '2 days' ELSE a.due_at - interval '1 day' END,
       CASE a.title WHEN c.code || ' quiz 1' THEN 'LATE' ELSE 'ON_TIME' END
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code IN ('ECO-501', 'ECO-510') AND a.due_at < now();

INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1010', c.id, now() - (d || ' days')::interval, 'CONTENT_VIEW'
FROM lms.course c CROSS JOIN generate_series(4, 10, 3) AS d
WHERE c.code IN ('ECO-501', 'ECO-510') AND c.term = '2026-2';
