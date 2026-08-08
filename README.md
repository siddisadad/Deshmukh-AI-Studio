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

# Validate production-shaped compose locally (builds prod profile, healthcheck, teardown)
./scripts/deploy-dry-run.sh

# Fast workspace gate (lint, build, unit tests — no Docker)
./scripts/validate-workspace.sh

# Validate staging-shaped compose locally (prod API profile, separate API/UI ports, no GHCR)
./scripts/staging-dry-run.sh

# Post-deploy smoke (health + info; after staging/prod is up)
./scripts/post-deploy-smoke.sh http://localhost:8088

# Staging dogfood gates (env + health + smoke + optional internal metrics)
./scripts/staging-dogfood.sh http://localhost:8088
# Full Stripe/OIDC/manual checklist: docs/14-STAGING-DOGFOOD-GUIDE.md

# Optional Prometheus + Grafana (internal metrics; see monitoring/README.md)
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# Deploy staging from GHCR (requires docker login ghcr.io if private)
cp .env.example .env
./scripts/validate-staging-env.sh
export IMAGE_TAG=v0.2.13-beta
./scripts/staging-ghcr-deploy.sh
```

#### Cloud Agents

Repository environment config: `.cursor/environment.json` (install, start, dev-server terminals, ports). After changing it on `main`, trigger an environment **Build** from the default branch, then **Save** in Cursor → Cloud Agents → Environment. Terminals and ports are defined in the committed JSON; the Save dialog only needs `install` and `start` scripts linked to a successful build.

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
- [x] Billing plans + entitlements (FREE/PRO/TEAM, mock Stripe checkout, project & AI daily limits; Stripe adapter env-gated)
- [x] SSO (OIDC-shaped port + mock provider, login callback; OIDC adapter env-gated)
- [x] Plugin / assistant-tool SPI (built-in assistants as plugins, sample tool, org enablement)
- [x] Phase 5 beta: growth E2E, prod/staging compose dry-runs, GHCR staging deploy script
- [x] Cloud Agent environment (validated `environment.json`, CI `environment-config`)
- [x] Observability: JSON logs, health probes, Prometheus metrics (internal), post-deploy smoke script
- [x] SSE chat reconnect/retry + cancel in-flight stream
- [x] Grafana/Prometheus monitoring overlay + staging dogfood script
- [x] Thread search in chat; Alertmanager + Loki log shipping
- [x] Read-only conversation share links (public read-only URL)
- [x] Horizontal job worker replicas (SKIP LOCKED claiming)
- [x] Background job queue depth Prometheus metrics + Grafana panel
- [x] Job worker autoscaling playbook + `worker-scale-hint.sh`
- [x] Loki off-site export script + optional S3 object-store backend
- [x] Conversation thread export (JSON / Markdown download)
- [x] Bulk project thread archive export (JSON / Markdown)
- [x] Dedicated background job worker container (staging GHCR deploy + dry-runs)
- [x] Workspace validation script + CI lint gate; Loki log-based alerts

**Release:** [CHANGELOG.md](CHANGELOG.md) — tag `v0.2.13-beta` adds worker autoscaling playbook; `v0.2.12-beta` added Loki S3 archive.

## Docs

Start at **[docs/README.md](docs/README.md)**.
