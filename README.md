# AI Studio for Software Engineering

AI-powered **engineering workspace** for planning, developing, testing, and documenting software with specialized AI assistants — not a chatbot, not an IDE.

## Tracks

### 1. Production MVP (Spring Boot + React) — active development

| Layer | Stack |
|---|---|
| API | Java 21, Spring Boot 3.3, Security JWT, JPA, Flyway, PostgreSQL |
| UI | React, TypeScript, MUI, React Router, React Query, Zustand |
| Design | [docs/README.md](docs/README.md) |

#### Quick start (local)

```bash
# Postgres (example)
# createuser/db already documented in docs/13-DEPLOYMENT-GUIDE.md

# API
cd backend
mvn spring-boot:run

# UI (another terminal)
cd frontend
npm install
npm run dev
```

- API: http://localhost:8080  
- Swagger: http://localhost:8080/swagger-ui.html  
- UI: http://localhost:5173  

Auth endpoints live under `/api/v1/auth/*` and `/api/v1/me`.

AI provider (default `mock`):

```bash
export AI_PROVIDER=openai          # or anthropic | mock
export OPENAI_API_KEY=sk-...
# export ANTHROPIC_API_KEY=...
export AI_RATE_LIMIT_PER_MINUTE=30
```

Optional Compose (when Docker is available):

```bash
cp .env.example .env
docker compose up --build

# Production-shaped (edge on :80, internal DB/API, Spring prod profile)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build

# Backup / restore
./scripts/backup-db.sh
./scripts/restore-db.sh ./backups/aistudio-YYYYMMDD-HHMMSS.sql.gz
```

### 2. Prototype (FastAPI shared-context proof)

Located under [`prototype/`](prototype/). See [SRS.md](SRS.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

```bash
cd prototype/backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
# open prototype/frontend/index.html
```

---

## Current MVP status

- [x] Design pack (PRD → deployment)
- [x] Backend foundations + JWT auth (register/login/refresh/logout/forgot-reset/profile)
- [x] React auth flow + dashboard shell
- [x] Projects CRUD, archive/unarchive, org listing, dashboard aggregates
- [x] Requirements CRUD + BA AI (improve / user stories / acceptance criteria)
- [x] Kanban tasks (status/priority/labels)
- [x] AI chat (4 assistants, shared context, conversation memory)
- [x] Documents CRUD + Documentation Writer generate
- [x] OpenAI / Anthropic providers (env-gated; default mock)
- [x] AI endpoint rate limiting
- [x] Project context assets (DB design / API spec / source metadata)
- [x] Prod compose + nginx security headers + backup/restore scripts
- [x] Empty-state / first-run cues on core pages
- [x] Streaming AI chat (SSE) with progressive tokens
- [x] Multi-thread conversations per assistant
- [x] RAG knowledge index (pgvector + mock/OpenAI embeddings)
- [x] Background AI jobs (reindex + document generate queue)
- [x] Billing plans + entitlements (FREE/PRO/TEAM, mock Stripe checkout, project & AI daily limits)

## Docs

Start at **[docs/README.md](docs/README.md)**.
