# 07 — Sprint Plan

## Sprint 0 — Environment setup

**Objective:** Every trainee can run the development environment.

### Install

- Python
- VS Code
- Git + GitHub access
- MySQL + MySQL Workbench
- Postman
- Node.js
- Chrome
- Docker — later / if required

### Verify

```bash
python --version
git --version
mysql --version
node --version
npm --version
```

---

## Sprint 1 — Python foundation

Topics: variables, types, strings, lists/tuples/sets/dicts, conditions, loops, functions, modules, classes, exceptions, files, venv, pip.

### Assignment

CLI program managing trainee records (dict-based), with:

- Add trainee
- Search trainee
- Update trainee
- Delete trainee
- Display trainees

Example record:

```python
trainee = {
    "name": "Rahul",
    "email": "rahul@example.com",
    "batch": "DTTP-01",
}
```

---

## Sprint 2 — Virtual environment

```bash
mkdir dttp-tms
cd dttp-tms
python -m venv venv
# Windows: .\venv\Scripts\Activate.ps1
# macOS/Linux: source venv/bin/activate
python -m pip install --upgrade pip
python --version
pip --version
```

---

## Sprint 3 — FastAPI hello world

```bash
pip install fastapi uvicorn
```

```
backend/
└── app/
    └── main.py
```

Minimal app: title `DTTP Trainee Management System`, `GET /` returns running message.

```bash
uvicorn app.main:app --reload
```

- App: http://127.0.0.1:8000  
- Swagger: http://127.0.0.1:8000/docs  

**Learning objective:** HTTP request → FastAPI router → Python function → response.

---

## Sprint 4 — REST API concepts

Teach GET / POST / PUT / PATCH / DELETE with trainee-shaped routes (in-memory is OK before DB).

---

## Sprint 5 — MySQL

- Create database `dttp_tms`
- Design initial tables: users, trainees, mentors, batches
- Implement trainee table fields (see [03-DATABASE-DESIGN.md](03-DATABASE-DESIGN.md))

---

## Sprint 6 — SQLAlchemy

```bash
pip install sqlalchemy pymysql
```

Wire: FastAPI → Router → Service → SQLAlchemy → MySQL  
Adopt the recommended backend package structure from [02-SYSTEM-ARCHITECTURE.md](02-SYSTEM-ARCHITECTURE.md).

---

## Sprint 7 — Trainee CRUD (first real milestone)

| Method | Path |
|---|---|
| POST | `/api/trainees` |
| GET | `/api/trainees` |
| GET | `/api/trainees/{id}` |
| PUT | `/api/trainees/{id}` |
| DELETE | `/api/trainees/{id}` |

Include validation + meaningful HTTP errors. Details: [13-FIRST-ASSIGNMENT-TRAINEE-CRUD.md](13-FIRST-ASSIGNMENT-TRAINEE-CRUD.md).

---

## Sprint 8 — Authentication

- Login
- Password hashing
- JWT
- Authorization by role (`ADMIN`, `MENTOR`, `TRAINEE`)
- Protect trainee APIs with Bearer token

---

## Sprint 9 — React frontend

Create `frontend/` with TypeScript React tooling. Build role-based screen shells (see [08-FRONTEND-SCREENS.md](08-FRONTEND-SCREENS.md)).

---

## Sprint 10 — Batch management

CRUD for batches + assign trainees/mentors as scoped in the sprint issue.

---

## Sprint 11 — Attendance

Statuses: Present · Absent · Late · Leave  
APIs: `POST/GET /api/attendance`  
Reports: daily, monthly, attendance %.

---

## Sprint 12 — Task management

Flow:

```
Mentor Creates Task
       ↓
Trainee Receives Task
       ↓
Trainee Works
       ↓
Trainee Submits
       ↓
Mentor Reviews
       ↓
Feedback
       ↓
Completed
```

Statuses: `TODO` → `IN_PROGRESS` → `SUBMITTED` → `REVIEW` → `COMPLETED`

---

## Sprint 13 — Performance & feedback

Mentor scores and feedback across technical and soft-skill dimensions (see database doc).

---

## After feature sprints

| Focus | Doc |
|---|---|
| Dashboard & reports | [08-FRONTEND-SCREENS.md](08-FRONTEND-SCREENS.md) |
| Testing | [09-TESTING-STRATEGY.md](09-TESTING-STRATEGY.md) |
| Docker / CI/CD / deploy | [05-DEVELOPMENT-ROADMAP.md](05-DEVELOPMENT-ROADMAP.md) Phases 12–14 |
