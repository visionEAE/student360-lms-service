-- Contract v2: S-1003's courses move from the generic engineering set to ones matching her
-- Psychology gradebook in core-service (same codes, so the 360 view's academic and engagement
-- tables describe the same five courses); three advisees without a login are added so the
-- advisor overview and the batch endpoints have more than one student to show.
--
-- Deviation from the raw design mock, recorded in docs/api-contract-v2.md: the mockup's headline
-- "18 days since last access" and "2 of 5 courses without activity" contradict its own per-course
-- table, where every course was touched within the last 20 days. A system cannot honestly report
-- both at once, so the seed keeps the per-course days-since-access values the mock shows (3, 18,
-- 5, 20, 12) and lets the overall signals be computed from them honestly: the most recent of the
-- five (3 days) is the true "days since last access", and none of the five courses is idle inside
-- the 30-day window, so "courses without activity" is 0.

DELETE FROM lms.submission  WHERE student_reference = 'S-1003';
DELETE FROM lms.access_log  WHERE student_reference = 'S-1003';
DELETE FROM lms.course_enrollment WHERE student_reference = 'S-1003';

INSERT INTO lms.course (code, name, term) VALUES
    ('PSI-301', 'Psicopatología',             '2026-2'),
    ('PSI-310', 'Neuropsicología',            '2026-2'),
    ('PSI-320', 'Psicología Organizacional',  '2026-2'),
    ('EST-201', 'Estadística II',             '2026-2'),
    ('HUM-210', 'Ética Profesional',          '2026-2'),
    ('MED-501', 'Medicina Interna',           '2026-2'),
    ('MED-510', 'Farmacología Clínica',       '2026-2'),
    ('MED-520', 'Salud Pública',              '2026-2'),
    ('PSI-401', 'Psicología Clínica',         '2026-2'),
    ('PSI-410', 'Evaluación Psicológica',     '2026-2'),
    ('PSI-420', 'Psicología Social',          '2026-2'),
    ('DIS-301', 'Diseño de Interacción',      '2026-2'),
    ('DIS-310', 'Tipografía',                 '2026-2'),
    ('DIS-320', 'Diseño de Producto',         '2026-2');

INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT 'S-1003', c.id, 'ACTIVE' FROM lms.course c
WHERE c.code IN ('PSI-301', 'PSI-310', 'PSI-320', 'EST-201', 'HUM-210') AND c.term = '2026-2';

INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT 'S-1004', c.id, 'ACTIVE' FROM lms.course c
WHERE c.code IN ('MED-501', 'MED-510', 'MED-520') AND c.term = '2026-2';

INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT 'S-1005', c.id, 'ACTIVE' FROM lms.course c
WHERE c.code IN ('PSI-401', 'PSI-410', 'PSI-420', 'HUM-210') AND c.term = '2026-2';

INSERT INTO lms.course_enrollment (student_reference, course_id, status)
SELECT 'S-1006', c.id, 'ACTIVE' FROM lms.course c
WHERE c.code IN ('DIS-301', 'DIS-310', 'DIS-320', 'HUM-110') AND c.term = '2026-2';

-- Assignments: five per S-1003 course (to host the exact on-time/late/missing pattern below),
-- four per course for the other three students.
INSERT INTO lms.assignment (course_id, title, type, due_at)
SELECT c.id, c.code || ' ' || a.title, a.type, now() - a.age
FROM lms.course c
CROSS JOIN (VALUES
    ('homework 1', 'HOMEWORK', interval '25 days'),
    ('quiz 1',     'QUIZ',     interval '20 days'),
    ('homework 2', 'HOMEWORK', interval '14 days'),
    ('quiz 2',     'QUIZ',     interval '7 days'),
    ('project',    'PROJECT',  interval '2 days')) AS a (title, type, age)
WHERE c.code IN ('PSI-301', 'PSI-310', 'PSI-320', 'EST-201', 'HUM-210') AND c.term = '2026-2';

INSERT INTO lms.assignment (course_id, title, type, due_at)
SELECT c.id, c.code || ' ' || a.title, a.type, now() - a.age
FROM lms.course c
CROSS JOIN (VALUES
    ('homework 1', 'HOMEWORK', interval '22 days'),
    ('quiz 1',     'QUIZ',     interval '15 days'),
    ('homework 2', 'HOMEWORK', interval '8 days'),
    ('project',    'PROJECT',  interval '2 days')) AS a (title, type, age)
WHERE c.code IN ('MED-501', 'MED-510', 'MED-520', 'PSI-401', 'PSI-410', 'PSI-420', 'DIS-301', 'DIS-310', 'DIS-320')
  AND c.term = '2026-2';

-- S-1003: onTime 13, late 5, missing 7 overall (rate 0.52), distributed per course to match the
-- design's own per-course table exactly (Gerencia Financiera -> PSI-301, Estrategia de Mercadeo ->
-- PSI-310, Comportamiento Organizacional -> PSI-320, Estadística II -> EST-201, Ética Empresarial
-- -> HUM-210). Every course has all five of its assignments graded, one way or another.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1003',
       CASE WHEN a.title LIKE '%project' THEN a.due_at + interval '2 days' ELSE a.due_at - interval '1 day' END,
       CASE WHEN a.title LIKE '%project' THEN 'LATE' ELSE 'ON_TIME' END
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code = 'PSI-301' AND a.due_at < now();           -- 4 on time, 1 late

INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1003', a.due_at - interval '1 day', 'ON_TIME'
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code = 'PSI-320' AND a.due_at < now();           -- 5 on time

INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1003',
       CASE WHEN a.title LIKE '%homework 1' THEN a.due_at - interval '1 day'
            WHEN a.title LIKE '%quiz%' THEN a.due_at + interval '2 days'
            ELSE NULL END,
       CASE WHEN a.title LIKE '%homework 1' THEN 'ON_TIME'
            WHEN a.title LIKE '%quiz%' THEN 'LATE'
            ELSE 'MISSING' END
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code = 'PSI-310' AND a.due_at < now();           -- 1 on time, 2 late, 2 missing

INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1003',
       CASE WHEN a.title LIKE '%homework 1' THEN a.due_at + interval '3 days' ELSE NULL END,
       CASE WHEN a.title LIKE '%homework 1' THEN 'LATE' ELSE 'MISSING' END
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code = 'EST-201' AND a.due_at < now();           -- 1 late, 4 missing

INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1003',
       CASE WHEN a.title LIKE '%homework%' OR a.title LIKE '%quiz 1' THEN a.due_at - interval '1 day'
            WHEN a.title LIKE '%quiz 2' THEN a.due_at + interval '2 days'
            ELSE NULL END,
       CASE WHEN a.title LIKE '%homework%' OR a.title LIKE '%quiz 1' THEN 'ON_TIME'
            WHEN a.title LIKE '%quiz 2' THEN 'LATE'
            ELSE 'MISSING' END
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code = 'HUM-210' AND a.due_at < now();           -- 3 on time, 1 late, 1 missing

-- S-1003 access log: one entry per course at the exact "days ago" the design shows, plus a
-- second, older entry on PSI-301 so the 30-day access count reaches 6 (matching the design).
INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1003', c.id, now() - d.age, 'CONTENT_VIEW'
FROM lms.course c
JOIN (VALUES
    ('PSI-301', interval '3 days'),
    ('PSI-301', interval '10 days'),
    ('PSI-310', interval '18 days'),
    ('PSI-320', interval '5 days'),
    ('EST-201', interval '20 days'),
    ('HUM-210', interval '12 days')) AS d (code, age) ON d.code = c.code
WHERE c.term = '2026-2';

-- S-1004 (Medicine): disengaged, mostly late or missing, no access in over three weeks.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1004',
       CASE WHEN a.title LIKE '%homework 1' THEN a.due_at - interval '1 day' ELSE NULL END,
       CASE WHEN a.title LIKE '%homework 1' THEN 'ON_TIME' ELSE 'MISSING' END
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code IN ('MED-501', 'MED-510', 'MED-520') AND a.due_at < now();

INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1004', c.id, now() - interval '23 days', 'LOGIN'
FROM lms.course c WHERE c.code = 'MED-501' AND c.term = '2026-2';

-- S-1005 (Psychology): academically at risk but engaged — daily access, on-time work.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1005', a.due_at - interval '1 day', 'ON_TIME'
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code IN ('PSI-401', 'PSI-410', 'PSI-420', 'HUM-210') AND a.due_at < now();

INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1005', c.id, now() - (d.day * interval '1 day') - interval '2 hours', 'CONTENT_VIEW'
FROM lms.course c CROSS JOIN generate_series(1, 5) AS d (day)
WHERE c.code IN ('PSI-401', 'PSI-410', 'PSI-420', 'HUM-210') AND c.term = '2026-2';

-- S-1006 (Design): everything on track — engaged, everything on time.
INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1006', a.due_at - interval '1 day', 'ON_TIME'
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code IN ('DIS-301', 'DIS-310', 'DIS-320') AND a.due_at < now();

INSERT INTO lms.submission (assignment_id, student_reference, submitted_at, status)
SELECT a.id, 'S-1006', a.due_at - interval '1 day', 'ON_TIME'
FROM lms.assignment a JOIN lms.course c ON c.id = a.course_id
WHERE c.code = 'HUM-110' AND a.due_at < now();

INSERT INTO lms.access_log (student_reference, course_id, occurred_at, access_type)
SELECT 'S-1006', c.id, now() - (d.day * interval '1 day') - interval '1 hour', 'CONTENT_VIEW'
FROM lms.course c CROSS JOIN generate_series(1, 4) AS d (day)
WHERE c.code IN ('DIS-301', 'DIS-310', 'DIS-320', 'HUM-110') AND c.term = '2026-2';
