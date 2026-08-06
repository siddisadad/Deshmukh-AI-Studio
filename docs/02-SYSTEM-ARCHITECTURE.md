# System Architecture Document
## AI Studio for Software Engineering — MVP

| Field | Value |
|---|---|
| **Companion** | `01-PRD.md` |
| **Style** | Modular monolith, Clean Architecture |
| **Stack** | Java 21, Spring Boot 3.x, PostgreSQL, Redis (optional), React + TypeScript + MUI |

---

## 1. Goals and Constraints

### Goals
- Deliver a production-shaped MVP that is secure, maintainable, and extensible.
- Keep a **single deployable backend** (modular monolith) to minimize ops cost for a startup.
- Make AI providers and assistants pluggable without rewriting domain code.
- Share one **project context** across all assistants.

### Constraints
- Small engineering team → prefer proven Spring/React patterns over microservices.
- AI latency and cost are external; architecture must isolate providers.
- MVP may run on a single VM via Docker Compose; design must allow later split of AI workers.

---

## 2. Architectural Style

**Modular monolith** with Clean Architecture boundaries inside one Spring Boot process:

```
┌──────────────────────────────────────────────────────────────────┐
│                         Clients                                  │
│              React SPA (MUI)  ·  Future API clients              │
└─────────────────────────────┬────────────────────────────────────┘
                              │ HTTPS / JSON
┌─────────────────────────────▼────────────────────────────────────┐
│                         Nginx                                    │
│              TLS termination · static SPA · reverse proxy        │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│                    Spring Boot API (modular monolith)            │
│  ┌────────────┐ ┌────────────┐ ┌──────────┐ ┌─────────────────┐  │
│  │ Identity   │ │ Projects   │ │ Work     │ │ AI              │  │
│  │ Auth/RBAC  │ │ Org/Proj   │ │ Req/Task │ │ Assistants      │  │
│  │ Profile    │ │ Dashboard  │ │ Docs     │ │ Context/RAG*    │  │
│  └─────┬──────┘ └─────┬──────┘ └────┬─────┘ └────────┬────────┘  │
│        │              │             │                │           │
│  ┌─────▼──────────────▼─────────────▼────────────────▼────────┐  │
│  │              Application services + domain                   │  │
│  └────────────────────────────┬─────────────────────────────────┘  │
│  ┌────────────────────────────▼─────────────────────────────────┐  │
│  │  Adapters: JPA · JWT · Mail · AI Providers · Redis (opt)     │  │
│  └────────────────────────────┬─────────────────────────────────┘  │
└───────────────────────────────┼──────────────────────────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
         PostgreSQL          Redis*          LLM APIs
         (+ Flyway)        (rate/cache)   (OpenAI/Anthropic)
```

\* Redis optional for MVP (rate limiting / token blacklist / cache).  
\* RAG interfaces present; vector DB deferred.

---

## 3. Clean Architecture Layers

```
com.aistudio
├── api          # Controllers, request/response DTOs, OpenAPI, filters
├── application  # Use cases / application services, ports (interfaces)
├── domain       # Entities, value objects, domain services, domain events
├── infrastructure
│   ├── persistence   # JPA entities (or domain entities mapped), repos, Flyway
│   ├── security      # JWT, filters, password encoder
│   ├── ai            # Provider implementations, prompt templates
│   ├── mail          # Email adapter
│   └── config        # Spring configuration
└── shared            # Cross-cutting: errors, logging, pagination
```

### Dependency rule
`api` → `application` → `domain` ← `infrastructure`

Infrastructure implements application ports (e.g. `AiProviderPort`, `EmailPort`, `ProjectRepository`).

---

## 4. Module Boundaries

| Module | Owns | Key aggregates |
|---|---|---|
| **Identity** | Users, credentials, password reset, profile | `User`, `RefreshToken`, `PasswordResetToken` |
| **Organization** | Orgs, membership, roles | `Organization`, `Membership` |
| **Project** | Projects, archive, dashboard aggregates | `Project`, `ProjectMember` |
| **Requirements** | Requirements, stories, AC | `Requirement` |
| **Tasks** | Tasks, labels, board | `Task`, `Label` |
| **Documents** | Docs content | `Document` |
| **AI** | Assistants, conversations, context, providers | `Conversation`, `Message`, `AssistantDefinition` |
| **Audit** | Security/business audit trail | `AuditLog` |

Modules communicate via application services or domain events—not by reaching into each other's persistence tables from controllers.

---

## 5. Request Lifecycle

### 5.1 Authenticated CRUD
1. Nginx → Spring Security filter chain.
2. JWT validated; `Authentication` populated with userId + authorities.
3. Controller validates DTO (`jakarta.validation`).
4. Application service enforces RBAC against org/project membership.
5. Domain mutation + JPA persist.
6. MapStruct maps entity → response DTO.
7. Structured log with `requestId`.

### 5.2 AI action / chat
1. Same authz as above.
2. `ContextBuilder` loads project snapshot (requirements, tasks, docs, metadata) under token budget.
3. `ConversationManager` loads recent messages for assistant.
4. `PromptManager` selects system prompt + action template.
5. `AiService` calls `AiProviderPort.generate(...)` (or stream later).
6. Persist messages / AI fields; return response.
7. On provider failure: map to `502/503` with retry-safe error code; never leak provider secrets.

---

## 6. Security Architecture

| Control | Implementation |
|---|---|
| Authentication | JWT access token (short-lived) + refresh token (httpOnly cookie or secure body storage) |
| Password storage | BCrypt (strength ≥ 12) or Argon2id |
| Authorization | Method security + service-level checks (`OWNER/ADMIN/MEMBER/VIEWER`) |
| CORS | Explicit allowed origins from config |
| Rate limiting | Per-IP / per-user buckets (Bucket4j + Redis or in-memory for single node) |
| Input validation | Bean Validation on all write DTOs; sanitize markdown where rendered |
| File upload | Allowlist MIME + max size + virus-scan hook (stub); store outside web root |
| Audit | Login success/fail, password reset, role change, project archive |
| Secrets | Env vars / Docker secrets; never commit keys |

### RBAC matrix (MVP)

| Action | OWNER | ADMIN | MEMBER | VIEWER |
|---|---|---|---|---|
| Manage org members | ✓ | ✓ | | |
| Create project | ✓ | ✓ | ✓ | |
| Edit project | ✓ | ✓ | ✓ | |
| Archive project | ✓ | ✓ | | |
| Edit requirements/tasks/docs | ✓ | ✓ | ✓ | |
| Invoke AI | ✓ | ✓ | ✓ | |
| Read project data | ✓ | ✓ | ✓ | ✓ |

---

## 7. Data Architecture

- **PostgreSQL** as system of record.
- **Flyway** for versioned migrations.
- Soft deletes / archive flags for projects (and optionally users).
- Conversations scoped by `(project_id, assistant_role, user_id)` or shared project thread per assistant (MVP: per project + assistant; optional user filter).
- See `03-DATABASE-DESIGN.md` for ERD and SQL.

---

## 8. AI Subsystem Overview

```
AiController
    → AiApplicationService
        → ContextBuilderPort
        → ConversationPort
        → PromptManager
        → AiProviderPort  → OpenAiProvider | AnthropicProvider | MockProvider
```

Each assistant definition includes: name, role key, system prompt, capabilities, limitations, tools (MVP: none or stub), context policy.

Full design: `07-AI-ARCHITECTURE.md`.

---

## 9. Frontend Architecture

- React + TypeScript SPA.
- React Router for pages listed in PRD.
- React Query for server state; Zustand for UI/session chrome.
- MUI with light/dark theme tokens.
- Axios/fetch client with auth interceptor (refresh flow).
- Feature folders mirroring domains (auth, projects, requirements, tasks, chat, documents).

Details: `06-FRONTEND-STRUCTURE.md`.

---

## 10. Infrastructure Topology (MVP)

```
docker-compose:
  nginx        :80/:443
  frontend     : build static → nginx
  api          :8080
  postgres     :5432
  redis        :6379 (optional profile)
```

CI: GitHub Actions — build, test, image publish (optional), compose smoke.

---

## 11. Cross-Cutting Concerns

| Concern | Approach |
|---|---|
| Errors | Global `@ControllerAdvice`; problem+json or consistent `{code,message,details}` |
| Logging | SLF4J + JSON encoder; MDC `requestId`, `userId`, `projectId` |
| Idempotency | Optional `Idempotency-Key` on AI POST (Should) |
| Pagination | Cursor or page/size for lists; default page size 20 |
| Time | UTC `timestamptz` everywhere |
| IDs | UUID primary keys |

---

## 12. Scalability Path (Post-MVP)

1. Extract AI worker queue (Postgres LISTEN/NOTIFY or Redis/Rabbit) for long generations.
2. Read replicas for heavy dashboard queries.
3. Object storage (S3) for uploads.
4. Vector DB for RAG.
5. Split Identity service only if multi-product SSO demands it.

Do **not** split modules into microservices until operational pain is measured.

---

## 13. Prototype Relationship

The existing FastAPI + SQLite prototype (`backend/`, `frontend/index.html`) validates:

- Shared project context mechanic.
- Four assistant roles.
- Provider abstraction (Mock/Anthropic).

This architecture **replaces** that stack for the production MVP while preserving those product mechanics. Mapping:

| Prototype | Production |
|---|---|
| FastAPI `main.py` | Spring modular packages |
| SQLite | PostgreSQL + Flyway |
| Static HTML | React + MUI |
| No auth | JWT + RBAC |
| In-request AI only | Same for MVP; queue-ready |

---

## 14. Quality Attributes Traceability

| NFR | Architectural decision |
|---|---|
| Maintainability | Clean Architecture + modules |
| Security | Spring Security JWT + RBAC + audit |
| Extensibility | `AiProviderPort`, assistant registry |
| Operability | Docker Compose, health endpoints, Flyway |
| Testability | Ports/adapters; Mock AI provider |

---

## 15. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Initial system architecture for MVP |

**Previous:** `01-PRD.md` · **Next:** `03-DATABASE-DESIGN.md`
