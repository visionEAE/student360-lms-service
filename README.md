# student360-lms-service

Simulated **learning platform** for Student 360° (port **8083**, schema **`lms`**): courses,
enrollment, assignments, submissions and access logs — behavioural data, high frequency, a
*signal* rather than an official fact. It is a separate service from core-service on purpose
(`student360-infra/docs/context.md` §4): different cadence, different nature, and at Icesi the LMS
is a third-party system with its own API.

| Method | Path | Audit action |
|---|---|---|
| `GET` | `/api/lms/students/{id}/courses` | `READ_COURSES` |
| `GET` | `/api/lms/students/{id}/activity?days=30` | `READ_ACTIVITY` |
| `GET` | `/api/lms/students/{id}/signals` | `READ_ENGAGEMENT_SIGNALS` |

Every `/api/**` call must carry a service token whose audience is `lms-service`; the user
identity arrives as `X-User-*` headers. Authorization is the shared
`StudentRecordAccessPolicy` (`SELF` / `STAFF_ROLE` / `ADMIN_ROLE`, otherwise `403` with a
`DENIED` audit record), applied before existence is revealed — same contract as core-service.

## The `/signals` contract

`EngagementSignalService` computes the interpretation once, inside the service that owns the
domain; support-service consumes it and never re-derives it.

```json
{
  "studentId": "S-1003",
  "computedAt": "2026-08-30T12:00:00Z",
  "daysSinceLastAccess": 21,        // null if never accessed
  "onTimeSubmissionRate": 0.22,     // 0..1, two decimals; null if nothing graded
  "coursesWithoutActivity": 2,      // active courses with no access in the last 30 days
  "activeCourses": 3,
  "lateSubmissions": 3,
  "missingSubmissions": 4
}
```

## Seed (relative to `now()`)

`S-1001` engaged — every course touched daily, all on time (rate 1.00, 0 idle courses) ·
`S-1002` in between — last access 5 days ago, quizzes late · `S-1003` **disengaged** — last
access 21 days ago, one course touched in 30 days, 3 late and 4 missing (rate 0.22). `S-1003` is
the at-risk student core-service also seeds: the convergence is what the support rule detects.

## Run · Verify

```bash
cd ../student360-infra && make up && make build-common && make run-lms-service
mvn verify   # format, style, unit tests of the signal logic, Testcontainers tests = phase gate 4
```
