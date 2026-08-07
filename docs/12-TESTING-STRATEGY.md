# Testing Strategy
## AI Studio for Software Engineering — MVP

Goal: confidence in authz, core CRUD, and AI orchestration without flaky external LLM calls.

---

## 1. Test Pyramid

```
        ┌────────────┐
        │   E2E few  │  Playwright (critical journeys)
        ├────────────┤
        │ API / IT   │  SpringBootTest + Testcontainers
        ├────────────┤
        │  Unit many │  Services, ContextBuilder, mappers
        └────────────┘
```

| Layer | Share (MVP) | Speed |
|---|---|---|
| Unit | ~70% | Fast |
| Integration/API | ~25% | Medium |
| E2E | ~5% | Slow |

---

## 2. Backend Unit Tests

**Focus**
- `AuthService` password hashing, token issuance logic (with mocked ports)
- `ProjectAuthorizationService` allow/deny matrix
- `ContextBuilder` truncation and section ordering
- `PromptTemplateManager` variable rendering
- `AiOrchestrationService` persistence calls with Mock provider
- MapStruct mappings for critical DTOs

**Tools:** JUnit 5, Mockito, AssertJ.

**Rules**
- No Spring context in pure unit tests unless necessary.
- Deterministic time via clocks where needed.

---

## 3. Backend Integration Tests

**Focus**
- Flyway migrates cleanly on Testcontainers Postgres
- Auth register/login/refresh/logout
- Cross-tenant access denied for projects/requirements/tasks
- Requirement AI action with Mock provider persists fields
- Chat message round-trip persists USER+ASSISTANT
- Validation → 400 error envelope
- Rate limit optional (profile-specific)

**Tools:** `@SpringBootTest`, MockMvc or WebTestClient, Testcontainers, Spring Security Test.

**Data:** per-test cleanup or transactional rollback where safe; prefer isolated schemas/containers.

---

## 4. Frontend Tests

**Unit/component (Vitest + RTL)**
- Login form validation
- ProtectedRoute redirect
- Kanban column render + status change callback
- Chat composer disable while sending
- Markdown rendering sanitization (if using raw HTML)

**API mocking:** MSW.

**E2E (Playwright) — smoke** (`e2e/`)
1. Register → create project → add requirement  
2. Create task → move to IN_PROGRESS  
3. Add document  
4. Streaming chat (SSE) + multi-thread isolation (Mock backend)  
5. RAG knowledge search after context asset save  
6. Billing overview (FREE plan)  
7. Plugins — disable sample Echo tool  
8. Logout  

**SSO** (`e2e/tests/sso.spec.ts`): mock provider login → dashboard (separate spec).

Run against compose (`E2E_BASE_URL=http://localhost:8088`) or local Vite (`http://localhost:5173`). CI `e2e` job uses compose + mock AI on every push/PR. CI `deploy-dry-run` and `staging-dry-run` validate compose overlays separately.

---

## 5. AI Testing Strategy

| Concern | Approach |
|---|---|
| Provider switch | Config tests assert bean type |
| Prompt regressions | Snapshot rendered prompts with fixed context fixture |
| Orchestration | Mock `AiProviderPort` returns canned text |
| Real provider | Manual/staging only; never required in CI |
| Hallucination UX | Assert UI shows AI badge / editable fields (FE tests) |

---

## 6. Security Test Cases (Must)

1. Unauthenticated request → 401  
2. Authenticated outsider → 403/404 on project resources  
3. VIEWER cannot mutate (if enforced)  
4. Reset token single-use  
5. Refresh token revoke on logout  
6. SQL/XSS payloads rejected or escaped  

---

## 7. Quality Gates (CI)

| Gate | Threshold (MVP) |
|---|---|
| Unit + IT | Must pass |
| Frontend unit | Must pass |
| Coverage (backend services) | Aim ≥ 70% on `application` package |
| Lint | Must pass |
| E2E smoke | Required on `main` |

Do not block MVP on 100% coverage; prioritize authz and AI orchestration.

---

## 8. Test Data & Environments

| Env | DB | AI |
|---|---|---|
| Local | Compose Postgres | mock |
| CI | Testcontainers | mock |
| Staging | Managed Postgres | real provider key in secret store |
| Prod | Managed Postgres | real provider |

Seed script for demos; not used in CI.

---

## 9. Non-Functional Checks

- k6 or simple script: login + list projects baseline (optional Sprint 7)
- Context builder performance with 50 requirements / 100 tasks fixture
- Bundle size watch on frontend build (informative)

---

## 10. Bug Triage Severity

| Severity | Example |
|---|---|
| P0 | Auth bypass, data leak across tenants |
| P1 | AI action corrupts requirement fields; Kanban not persisting |
| P2 | UI polish, prompt quality |
| P3 | Nice-to-have copy |

---

## 11. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | MVP testing strategy |

**Previous:** `11-CODING-STANDARDS.md` · **Next:** `13-DEPLOYMENT-GUIDE.md`
