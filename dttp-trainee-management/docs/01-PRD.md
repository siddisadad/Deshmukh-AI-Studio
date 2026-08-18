# 01 — Product Requirements (PRD)

## 1. Project objective

The **DTTP Trainee Management System (TMS)** manages the complete trainee lifecycle at Deshmukh Technologies.

Trainees build it as a production-style web application while learning:

- Python backend (FastAPI)
- React frontend (TypeScript)
- Relational database (MySQL + SQLAlchemy)
- Git / GitHub
- Testing
- Docker
- CI/CD

## 2. Initial capabilities

| Area | Capability |
|---|---|
| Access | User login |
| People | Trainee registration, mentor management |
| Structure | Batch management |
| Operations | Attendance, daily tasks, assignments |
| Quality | Submission tracking, feedback, performance tracking |
| Insight | Dashboard, reports |

**Scope control:** Not all capabilities ship in Sprint 1. Deliver a thin vertical slice first, then expand.

## 3. User roles

### Admin

- Create trainees
- Create mentors
- Create batches
- Assign trainees to batches
- Assign mentors
- View reports
- Manage users

### Mentor

- View assigned trainees
- Mark attendance
- Create tasks
- Review submissions
- Give feedback
- Track trainee progress

### Trainee

- View profile
- View batch
- View attendance
- View assigned tasks
- Submit assignments
- View feedback
- View progress

## 4. Non-goals (early sprints)

- Full HR / payroll
- Mobile native apps
- Complex BI / data warehouse
- Multi-tenant SaaS billing
- Production CI/CD on Day 1

## 5. Success criteria (Month 1 MVP)

By end of Month 1 the team should have:

```
Login → Dashboard → Trainee Management → Batch Management
  → MySQL → REST APIs → React UI → GitHub PR workflow
```

## 6. Principles

1. **MVP first** — working software over complete feature list  
2. **Learn by shipping** — each sprint ends in a demoable increment  
3. **Mentor review required** — no direct pushes to `main`  
4. **Definition of Done** — code + tests + docs + PR approval  
