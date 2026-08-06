# Sprint Planning
## AI Studio for Software Engineering — MVP

Assumes **2-week sprints**, team of ~3 full-stack engineers. Story points are relative (1/2/3/5/8). Adjust capacity (~30–40 pts/sprint).

---

## 1. Sprint 0 — Bootstrap

**Goal:** Runnable skeletons and CI.

| ID | Story | Pts | Notes |
|---|---|---|---|
| S0-1 | Init Spring Boot + packages | 3 | Clean Architecture folders |
| S0-2 | Init Vite React + MUI theme | 3 | Router shell |
| S0-3 | Docker Compose: Postgres (+ Redis opt) | 2 | |
| S0-4 | Flyway V1 schema | 5 | From DB design |
| S0-5 | GitHub Actions CI | 3 | |
| S0-6 | Health + OpenAPI stub | 2 | |

**Exit:** Compose up; CI green on empty tests.

---

## 2. Sprint 1 — Auth

**Goal:** Secure identity.

| ID | Story | Pts |
|---|---|---|
| S1-1 | Register + personal org | 5 |
| S1-2 | Login + JWT access/refresh | 5 |
| S1-3 | Logout + refresh rotation | 3 |
| S1-4 | Forgot/reset password | 5 |
| S1-5 | Profile GET/PATCH + password change | 3 |
| S1-6 | Frontend auth pages + store + guards | 8 |
| S1-7 | Auth integration tests | 3 |

**Exit:** Demo register → login → profile → logout.

---

## 3. Sprint 2 — Projects & RBAC

**Goal:** Multi-tenant project shell.

| ID | Story | Pts |
|---|---|---|
| S2-1 | Project CRUD + archive | 5 |
| S2-2 | Project membership + authz service | 5 |
| S2-3 | Dashboard aggregates API | 3 |
| S2-4 | Frontend dashboard + project layout | 8 |
| S2-5 | Org members list/add (basic) | 5 |
| S2-6 | Authz tests (cross-tenant deny) | 3 |

**Exit:** User A cannot read User B project (403/404).

---

## 4. Sprint 3 — Requirements & Documents

**Goal:** Core knowledge capture.

| ID | Story | Pts |
|---|---|---|
| S3-1 | Requirements CRUD API | 5 |
| S3-2 | Requirement editor UI | 8 |
| S3-3 | Documents CRUD API + UI | 8 |
| S3-4 | Context assets API | 3 |
| S3-5 | Markdown rendering component | 3 |

**Exit:** Create/edit requirements and docs in a project.

---

## 5. Sprint 4 — Tasks & Kanban

**Goal:** Delivery board.

| ID | Story | Pts |
|---|---|---|
| S4-1 | Labels API | 2 |
| S4-2 | Tasks CRUD + filters | 5 |
| S4-3 | Reorder/status batch endpoint | 3 |
| S4-4 | Kanban UI + drawer | 8 |
| S4-5 | Link task ↔ requirement | 2 |
| S4-6 | Optimistic updates + tests | 3 |

**Exit:** Move cards across four columns; persist reload.

---

## 6. Sprint 5 — AI Foundation

**Goal:** Provider + context + chat.

| ID | Story | Pts |
|---|---|---|
| S5-1 | AiProviderPort + Mock + config switch | 5 |
| S5-2 | Prompt templates on classpath | 3 |
| S5-3 | ContextBuilder with budgets | 5 |
| S5-4 | Conversations/messages API | 5 |
| S5-5 | Assistants catalog endpoint | 2 |
| S5-6 | AI Chat UI | 8 |
| S5-7 | One real provider (Anthropic or OpenAI) | 5 |

**Exit:** Chat with BA/Dev using shared context (mock OK for CI).

---

## 7. Sprint 6 — AI Actions & Hardening

**Goal:** MVP feature complete + production readiness.

| ID | Story | Pts |
|---|---|---|
| S6-1 | BA improve/stories/AC endpoints + UI | 8 |
| S6-2 | Docs generate + QA/Dev shortcuts | 5 |
| S6-3 | Rate limiting + audit logging | 5 |
| S6-4 | Global errors, CORS, security headers | 3 |
| S6-5 | Nginx + prod compose profile | 3 |
| S6-6 | Test suite expansion + coverage gates | 5 |
| S6-7 | Deploy guide dry-run | 2 |

**Exit:** PRD acceptance checklist satisfied for internal demo.

---

## 8. Sprint 7 — Beta Polish (optional buffer)

| ID | Story | Pts |
|---|---|---|
| S7-1 | Empty states + onboarding cues | 3 |
| S7-2 | Prompt tuning from dogfood | 5 |
| S7-3 | Performance pass (N+1, indexes) | 3 |
| S7-4 | Accessibility pass | 3 |
| S7-5 | Backup/restore script | 2 |

---

## 9. Ceremonies & Quality Bar

- **Planning:** refine only Must items from PRD.
- **Daily:** unblock AI provider/key issues early.
- **Review:** demo against wireframes.
- **Definition of Done:** tests, OpenAPI updated, no secrets committed, UX loading/error states present.

---

## 10. Risk Buffer

| Risk | Sprint impact | Buffer |
|---|---|---|
| JWT/refresh edge cases | S1–S2 | +3 pts |
| Kanban DnD complexity | S4 | Use status menus first |
| Provider SDK quirks | S5 | Mock-first demos |

---

## 11. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | 2-week sprint plan for MVP |

**Previous:** `09-DEVELOPMENT-ROADMAP.md` · **Next:** `11-CODING-STANDARDS.md`
