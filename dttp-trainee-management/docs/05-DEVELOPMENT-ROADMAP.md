# 05 — Development Roadmap

Ordered by risk reduction and learning. Each phase produces a demoable outcome.

## Phase overview

```
Phase 1   Environment + Git
    ↓
Phase 2   Python + FastAPI
    ↓
Phase 3   MySQL + SQLAlchemy
    ↓
Phase 4   Trainee CRUD
    ↓
Phase 5   Authentication
    ↓
Phase 6   React Frontend
    ↓
Phase 7   Frontend + API Integration
    ↓
Phase 8   Attendance
    ↓
Phase 9   Tasks & Assignments
    ↓
Phase 10  Dashboard & Reports
    ↓
Phase 11  Testing
    ↓
Phase 12  Docker
    ↓
Phase 13  CI/CD
    ↓
Phase 14  Deployment
```

## Phase outcomes

| Phase | Outcome |
|---|---|
| 1 | Tools installed; repo cloned; branch workflow understood |
| 2 | FastAPI app serves `/` and Swagger |
| 3 | MySQL database + SQLAlchemy models connected |
| 4 | Trainee CRUD APIs work (Postman) |
| 5 | Login + JWT + role-protected routes |
| 6 | React app shell with routes |
| 7 | UI calls trainee APIs end-to-end |
| 8 | Attendance mark + list |
| 9 | Tasks, submissions, feedback flow |
| 10 | Role dashboards + basic reports |
| 11 | pytest + Postman suites + UI smoke checks |
| 12 | `docker compose up` runs stack |
| 13 | GitHub Actions lint/test/build |
| 14 | Deployed environment + runbook |

## Rule

Do **not** jump to Docker/CI/CD before the app works locally and the MVP APIs are stable.
