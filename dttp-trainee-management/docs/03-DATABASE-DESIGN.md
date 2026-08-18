# 03 — Database Design

## 1. Database

```sql
CREATE DATABASE dttp_tms;
```

## 2. Core tables (initial)

Priority for early sprints: `users`, `trainees`, `mentors`, `batches`.

Later sprints add attendance, tasks, submissions, feedback, and performance scores.

## 3. Trainee (Sprint 5–7)

```
trainee
---------
id
name
email
mobile
batch_id
joining_date
status
created_at
updated_at
```

### Status values

| Status | Meaning |
|---|---|
| `ACTIVE` | Currently in program |
| `INACTIVE` | Temporarily inactive |
| `COMPLETED` | Finished program |
| `DROPPED` | Left program |

## 4. Batches (Sprint 10)

```
batches
---------
id
name
start_date
end_date
status
mentor_id
```

## 5. Users & roles (Sprint 8)

Authentication uses a `users` table (or equivalent) with roles:

| Role |
|---|
| `ADMIN` |
| `MENTOR` |
| `TRAINEE` |

Passwords must be stored hashed (never plain text). JWT is used for API access.

## 6. Attendance (Sprint 11) — planned

Example fields:

| Field | Notes |
|---|---|
| trainee_id | FK |
| date | Attendance day |
| status | `PRESENT` · `ABSENT` · `LATE` · `LEAVE` |
| marked_by | User id |
| remarks | Optional |

## 7. Tasks / assignments (Sprint 12) — planned

Mentor creates tasks with title, description, deadline, priority, assignee.

Status flow:

```
TODO → IN_PROGRESS → SUBMITTED → REVIEW → COMPLETED
```

## 8. Performance (Sprint 13) — planned

Example scored dimensions (e.g. /10):

- Technical skills (Python, FastAPI, Database, Git)
- Problem solving
- Communication
- Task completion / code quality
- Attendance
- Professionalism

## 9. Design notes for trainees

- Prefer clear FK relationships over duplicated strings where possible  
- Add `created_at` / `updated_at` on mutable entities  
- Enforce unique emails for trainees and users  
- Soft-deactivate (`INACTIVE`) is often safer than hard delete in early MVP  
