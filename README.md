# AI Studio for Software Engineering

AI-powered **engineering workspace** for planning, developing, testing, and documenting software with specialized AI assistants — not a chatbot, not an IDE.

## Two tracks in this repository

### 1. Production SaaS design (start here for the full MVP)

Complete design pack (PRD → deploy):

**[docs/README.md](docs/README.md)**

Stack target: Java 21 · Spring Boot · PostgreSQL · React · MUI · Docker.

### 2. Working prototype (shared-context proof)

Single-user local app: FastAPI + SQLite + static HTML/JS.

```
Backend (FastAPI + SQLite)  ←→  Frontend (static HTML/JS)
```

See [SRS.md](SRS.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
# Open frontend/index.html (or serve the frontend folder)
```

Optional: `export ANTHROPIC_API_KEY=...` for a real model; otherwise Mock provider.

---

## MVP modules (SaaS)

| Area | Capabilities |
|---|---|
| Auth | Register, login, JWT, forgot password, profile |
| Projects | Create, edit, archive, dashboard |
| Requirements | Editor + AI improve / user stories / AC |
| Tasks | Kanban, priority, status, labels |
| Assistants | Business Analyst, Developer, QA, Documentation Writer |
| Shared context | Project, requirements, tasks, docs, specs, conversations |

---

## Document order

1. PRD → 2. Architecture → 3. Database → 4. API → 5. Backend structure → 6. Frontend structure → 7. AI architecture → 8. Wireframes → 9. Roadmap → 10. Sprints → 11. Coding standards → 12. Testing → 13. Deployment
