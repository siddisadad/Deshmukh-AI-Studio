# Development Roadmap
## AI Studio for Software Engineering — MVP → Growth

Realistic roadmap for a small startup (2–4 engineers). Ordered by risk reduction and vertical slices.

---

## 1. Guiding Rules

1. Ship vertical slices (auth → project → requirement AI) over horizontal layers alone.
2. Keep the modular monolith until AI load or team size forces extraction.
3. Prefer Mock AI in CI; real providers in staging.
4. Every phase ends with demos against acceptance criteria in `01-PRD.md`.

---

## 2. Phase Overview

| Phase | Name | Outcome |
|---|---|---|
| **0** | Foundations | Repo, CI, Docker Compose, Flyway baseline, OpenAPI stub |
| **1** | Identity & tenancy | Register/login/JWT/RBAC/org/project shell |
| **2** | Work management | Requirements, tasks/Kanban, documents CRUD |
| **3** | AI workspace | Providers, context, assistants, chat, BA actions |
| **4** | Hardening | Rate limits, audit, tests, deploy guide validation |
| **5** | Beta | Staging, feedback, polish UX |
| **6+** | Growth | RAG, billing, SSO, plugins |

---

## 3. Phase 0 — Foundations

**Deliverables**
- Monorepo or dual folders: `backend/`, `frontend/`, `docs/`, `docker-compose.yml`
- Spring Boot skeleton + React Vite skeleton
- PostgreSQL + Flyway `V1`
- GitHub Actions: compile, unit tests, frontend lint/build
- Health endpoint + README run instructions

**Exit criteria:** `docker compose up` boots API + DB; empty SPA loads.

---

## 4. Phase 1 — Identity & Tenancy

**Deliverables**
- Register, login, refresh, logout, forgot/reset password
- Profile + theme preference
- Organizations + memberships
- Projects CRUD + archive + dashboard counts (zeros OK)
- Frontend auth flow + protected routes + app shell

**Exit criteria:** Two users cannot access each other’s projects (authz tests green).

---

## 5. Phase 2 — Work Management

**Deliverables**
- Requirements CRUD + editor UI
- Tasks + labels + Kanban
- Documents CRUD + markdown editor
- Context assets upsert API (UI minimal OK)

**Exit criteria:** Full non-AI SDLC path works: requirement → tasks → doc.

---

## 6. Phase 3 — AI Workspace

**Deliverables**
- `AiProviderPort` + Mock + one real provider
- Prompt templates + ContextBuilder + ConversationManager
- Four assistants listed and chatable
- BA improve / stories / AC
- Docs generate + Developer/QA shortcut endpoints (or chat-only if timebox)

**Exit criteria:** Same context influences BA and Developer answers; history persists.

---

## 7. Phase 4 — Hardening

**Deliverables**
- Rate limiting, CORS lockdown, audit logs
- Global error model, request IDs
- Unit + API integration tests for critical paths
- Nginx + compose production profile
- OpenAPI published artifact

**Exit criteria:** Security smoke checklist passed; deploy guide followed successfully on clean VM/local.

---

## 8. Phase 5 — Private Beta

**Deliverables**
- Staging environment + monitoring basics
- Onboarding empty states polished
- Bug bash + prompt tuning
- Backup script for Postgres
- Compose dry-runs (prod + staging local build) in CI
- GHCR staging deploy script; images published on `main`
- Growth E2E (multi-thread chat, RAG, billing, plugins, mock SSO)
- Cloud Agent environment build + validation

**Exit criteria:** Friendly users complete first-run journey without assistance.

**Status (2026-08-07):** Complete on `main` — see [CHANGELOG.md](../CHANGELOG.md) `v0.1.0-beta`.

---

## 9. Phase 6+ — Growth Backlog

Shipped on `main` (v0.1.0-beta baseline); remaining work is production hardening and depth.

| Item | Status on `main` | Next depth |
|---|---|---|
| RAG over documents/code metadata | pgvector index + search UI | Larger corpora, real embedding ops |
| Streaming AI responses | SSE chat + reconnect/retry UX | Token UX polish |
| Observability | JSON logs, health, smoke, Prometheus, Grafana, Alertmanager, Loki, log alerts, 30d log retention | Off-site log archive / S3 backend |
| Billing / plans | Stripe checkout + webhook (env-gated) | Invoices, usage metering depth |
| SSO (OIDC/SAML) | Real OIDC adapter (env-gated) | IdP-specific guides ([14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) §6), SAML |
| Multi-thread conversations | Per-assistant threads + title/message search (`q`) | Thread sharing depth (private threads) |
| Plugin architecture | SPI + org toggles | Marketplace, third-party packs |
| Background AI jobs | Reindex + doc generate queue + dedicated worker + SKIP LOCKED multi-replica | Autoscaling / queue depth metrics |

---

## 10. Dependency Graph (simplified)

```
Foundations → Identity → Projects → Requirements/Tasks/Docs → AI → Hardening → Beta
```

AI can prototype in parallel behind feature flag after Projects exist, but do not merge without authz.

---

## 11. Staffing Suggestion

| Role | Focus |
|---|---|
| Backend lead | Security, modules, AI ports |
| Frontend lead | Shell, Kanban, chat UX |
| Full-stack | Requirements/docs slices |
| Part-time DevOps | Compose, CI, staging |

---

## 12. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | MVP-centered roadmap |

**Previous:** `08-UI-WIREFRAMES.md` · **Next:** `10-SPRINT-PLANNING.md`
