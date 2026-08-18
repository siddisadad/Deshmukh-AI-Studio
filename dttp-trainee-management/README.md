# DTTP Trainee Management System (TMS) — Documentation

Training curriculum and design pack for building a production-style **Trainee Management System** at Deshmukh Technologies.

**Purpose:** Trainees learn Python backend, React frontend, database, Git/GitHub, testing, Docker, and CI/CD by building a real product — starting with a small MVP and expanding sprint by sprint.

**Stack (target)**

```
React + TypeScript
        ↓
REST API
        ↓
Python + FastAPI
        ↓
SQLAlchemy
        ↓
MySQL
```

Supporting tools: Git + GitHub · Postman · Docker · GitHub Actions

---

## Single PDF

Download the combined curriculum:

**[DTTP-TMS-Curriculum.pdf](docs/DTTP-TMS-Curriculum.pdf)**

---

## Document index

| # | Document | Description |
|---|---|---|
| 1 | [01-PRD.md](docs/01-PRD.md) | Objectives, capabilities, user roles |
| 2 | [02-SYSTEM-ARCHITECTURE.md](docs/02-SYSTEM-ARCHITECTURE.md) | Target architecture & request flow |
| 3 | [03-DATABASE-DESIGN.md](docs/03-DATABASE-DESIGN.md) | Initial schema (users, trainees, mentors, batches, …) |
| 4 | [04-API-SPECIFICATION.md](docs/04-API-SPECIFICATION.md) | Planned REST endpoints |
| 5 | [05-DEVELOPMENT-ROADMAP.md](docs/05-DEVELOPMENT-ROADMAP.md) | Phases 1–14 |
| 6 | [06-GIT-AND-REPO-STRATEGY.md](docs/06-GIT-AND-REPO-STRATEGY.md) | Repo layout, branches, PR workflow |
| 7 | [07-SPRINT-PLAN.md](docs/07-SPRINT-PLAN.md) | Sprint 0–13 learning & delivery plan |
| 8 | [08-FRONTEND-SCREENS.md](docs/08-FRONTEND-SCREENS.md) | Screens by role |
| 9 | [09-TESTING-STRATEGY.md](docs/09-TESTING-STRATEGY.md) | Backend, API, frontend testing |
| 10 | [10-DEFINITION-OF-DONE.md](docs/10-DEFINITION-OF-DONE.md) | DoD, commits, PR standard |
| 11 | [11-30-AND-90-DAY-PLANS.md](docs/11-30-AND-90-DAY-PLANS.md) | Month 1 / 90-day roadmap |
| 12 | [12-TEAM-STRUCTURE.md](docs/12-TEAM-STRUCTURE.md) | Backend / Frontend / QA / DevOps |
| 13 | [13-FIRST-ASSIGNMENT-TRAINEE-CRUD.md](docs/13-FIRST-ASSIGNMENT-TRAINEE-CRUD.md) | First real milestone |

---

## Guiding rule

**Do not build everything in the first sprint.**

Start with a small working MVP (environment → FastAPI → MySQL → Trainee CRUD → auth → React), then expand to attendance, tasks, dashboard, testing, Docker, and CI/CD.

---

## Intended repository (when code starts)

```
dttp-trainee-management/
├── backend/
├── frontend/
├── docs/          ← this documentation lives here
├── database/
├── docker/
├── .gitignore
├── README.md
└── docker-compose.yml
```

This folder currently contains **documentation only**. Implementation begins after mentors approve the plan and open Sprint 0.
