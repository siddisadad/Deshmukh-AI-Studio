# Software Requirements Specification
## AI Studio for Software Engineering — Prototype

Version 0.1 · Covers the working prototype in `prototype/backend` and `prototype/frontend`

---

## 1. Introduction

### 1.1 Purpose
This SRS specifies the requirements for the **AI Studio prototype**: a
single-user web application in which software requirements and tasks share
a common project context that multiple role-specific AI assistants can read
from and respond to.

It intentionally covers the **prototype scope**, not the full multi-tenant
SaaS platform described in the original product vision. Section 8 maps every
deferred capability back to that larger vision so this document stays
traceable as the system grows.

### 1.2 Scope
In scope: Project management (create/list), Requirements (create, AI-assisted
improvement/user stories/acceptance criteria), Tasks (create, status/priority,
Kanban view), four AI Assistants (Business Analyst, Developer, QA Engineer,
Documentation Writer) each with role-scoped chat over shared project context,
and a pluggable AI provider (mock or Anthropic API).

Out of scope for this version: authentication/authorization, multi-user or
multi-tenant support, RAG/vector retrieval over source code, real-time
collaboration, notifications, file uploads, and any deployment automation.

### 1.3 Definitions and Acronyms
| Term | Meaning |
|---|---|
| Assistant | A named AI role (e.g. Business Analyst) with a fixed system prompt |
| Project Context | The concatenated text of a project's requirements + tasks, built fresh per request |
| Provider | The `AIProvider` implementation actually generating text (Mock or Anthropic) |
| SRS | This document |

### 1.4 References
- Original product vision brief: "AI Studio for Software Engineering" (full
  platform concept — Java/Spring/React/Postgres/RBAC/RAG). Referenced
  throughout as "the full vision."
- Prototype source: `prototype/backend/main.py`, `prototype/backend/ai_provider.py`,
  `prototype/frontend/index.html`.

### 1.5 Overview
Section 2 describes the product at a high level. Section 3 lists functional
requirements by module. Section 4 covers non-functional requirements.
Section 5 covers external interfaces. Section 6 covers data requirements.
Section 7 lists known limitations. Section 8 maps deferred scope to the full
vision.

---

## 2. Overall Description

### 2.1 Product Perspective
The prototype is a standalone two-tier application: a FastAPI backend
exposing a REST API over a SQLite database, and a single static HTML/JS
frontend calling that API directly from the browser. There is no separate
auth layer, message queue, or external storage — everything runs as one
backend process plus one static file.

### 2.2 Product Functions (summary)
1. Create and select projects.
2. Add requirements with a title and free-text description.
3. Trigger AI actions per requirement: improve description, generate user
   stories, generate acceptance criteria.
4. Add tasks with title/priority; move tasks across four statuses on a
   Kanban board.
5. Chat with any of four AI assistants; each assistant's replies are
   generated from the same shared project context (current requirements +
   tasks) plus a role-specific system prompt.
6. Switch the AI backend from a deterministic mock to a real Anthropic model
   via one environment variable, with no code change.

### 2.3 User Classes and Characteristics
Single class: **Individual developer or small-team member acting alone.**
No concept of roles, permissions, or shared/multi-user projects exists in
this version — every request implicitly belongs to "the" user.

### 2.4 Operating Environment
- Backend: Python 3.10+, runs locally via `uvicorn`.
- Frontend: any modern browser, opened as a local file, calling
  `http://localhost:8000`.
- Storage: a single SQLite file (`ai_studio.db`) created on first run.
- Optional external dependency: Anthropic API, only if `ANTHROPIC_API_KEY`
  is set.

### 2.5 Design and Implementation Constraints
- No build tooling for the frontend (plain HTML/JS, no npm/webpack).
- No ORM migrations — schema is created via `Base.metadata.create_all()`;
  schema changes require manual DB reset during development.
- Single SQLite writer — not designed for concurrent multi-process writes.
- AI provider calls are synchronous and blocking within the request/response
  cycle (no streaming, no background jobs).

### 2.6 Assumptions and Dependencies
- Assumes one browser tab operating against one running backend instance.
- Assumes the AI provider (mock or real) returns plain text within a single
  call; no support for multi-turn tool use or function calling.
- Assumes project/requirement/task volumes small enough that the context
  builder can safely truncate to the first 20 requirements and first 30
  tasks (see 4.1) without losing essential context.

---

## 3. Specific Requirements (Functional)

Each requirement has an ID, description, and priority (Must / Should / Could).

### 3.1 Project Management
| ID | Requirement | Priority |
|---|---|---|
| FR-1.1 | The system shall allow creating a project with a name and optional description. | Must |
| FR-1.2 | The system shall list all existing projects. | Must |
| FR-1.3 | The system shall allow retrieving a single project by ID. | Must |
| FR-1.4 | The user shall be able to select which project is "active" in the UI, scoping all other views to it. | Must |

### 3.2 Requirements Management
| ID | Requirement | Priority |
|---|---|---|
| FR-2.1 | The system shall allow creating a requirement (title + description) under a project. | Must |
| FR-2.2 | The system shall list all requirements for a project. | Must |
| FR-2.3 | The system shall generate an improved version of a requirement's description via the Business Analyst assistant, and persist it. | Must |
| FR-2.4 | The system shall generate user stories (As a/I want/So that) for a requirement via the Business Analyst assistant, and persist them. | Must |
| FR-2.5 | The system shall generate acceptance criteria for a requirement via the Business Analyst assistant, and persist them. | Must |
| FR-2.6 | Each AI-generated field (improved description, user stories, acceptance criteria) shall be displayed alongside the original requirement once generated. | Should |

### 3.3 Task Management
| ID | Requirement | Priority |
|---|---|---|
| FR-3.1 | The system shall allow creating a task (title, description, priority) under a project, optionally linked to a requirement. | Must |
| FR-3.2 | The system shall list all tasks for a project. | Must |
| FR-3.3 | The system shall allow updating a task's status, priority, labels, title, or description. | Must |
| FR-3.4 | The UI shall display tasks grouped into four columns by status: To Do, In Progress, Review, Done. | Must |
| FR-3.5 | The user shall be able to change a task's status from the board view. | Must |

### 3.4 AI Assistants
| ID | Requirement | Priority |
|---|---|---|
| FR-4.1 | The system shall expose exactly four assistants: Business Analyst, Developer, QA Engineer, Documentation Writer. | Must |
| FR-4.2 | Each assistant shall have a fixed, distinct system prompt describing its role and responsibilities. | Must |
| FR-4.3 | Every assistant response shall be generated using the same shared project-context builder (current requirements + tasks), so no assistant has privileged information another lacks. | Must |
| FR-4.4 | The system shall list available assistants via an API endpoint, used to populate the UI selector. | Must |

### 3.5 Chat
| ID | Requirement | Priority |
|---|---|---|
| FR-5.1 | The user shall be able to send a free-text message to a chosen assistant within the context of the active project. | Must |
| FR-5.2 | The system shall persist both the user's message and the assistant's reply as chat history, scoped by project and assistant role. | Must |
| FR-5.3 | The system shall allow retrieving the full chat history for a given project/assistant pair, ordered by time. | Must |
| FR-5.4 | Switching the assistant selector shall load that assistant's own history, not another assistant's. | Must |

### 3.6 AI Provider
| ID | Requirement | Priority |
|---|---|---|
| FR-6.1 | The system shall default to a deterministic mock AI provider requiring no external credentials. | Must |
| FR-6.2 | The system shall automatically use a real Anthropic-backed provider when `ANTHROPIC_API_KEY` is present in the environment, with no code or config changes required. | Must |
| FR-6.3 | The provider interface shall be implementation-agnostic, so a new provider (e.g. OpenAI) can be added by implementing one method (`generate`). | Should |

---

## 4. Non-Functional Requirements

### 4.1 Performance
- The context builder truncates to the first 20 requirements and first 30
  tasks per project to bound prompt size and response latency as data grows.
- With the mock provider, responses shall return in well under 1 second.
  With a real provider, latency is bounded by the external API and is not
  guaranteed by this system.

### 4.2 Usability
- All core actions (create project/requirement/task, trigger an AI action,
  send a chat message) shall be reachable within two clicks from app load.
- AI-generated content shall be visually distinguishable from user-entered
  content in both the requirement cards and the chat log.

### 4.3 Reliability
- Unknown assistant roles or missing projects/requirements/tasks shall
  return explicit HTTP error responses (400/404), not silent failures.
- If the `anthropic` package is requested but not installed, the system
  shall fall back to the mock provider rather than crashing at startup.

### 4.4 Security
- **Not implemented in this version** — see Section 7. No authentication,
  authorization, input sanitization beyond framework defaults, or rate
  limiting exists. This prototype is intended for local, single-user use
  only and must not be exposed on a public network as-is.

### 4.5 Maintainability
- Assistant identities (name + system prompt) live in one dictionary
  (`ASSISTANTS`) as the single source of truth, used by both the chat
  endpoints and the assistant-listing endpoint.
- The AI provider is behind an abstract interface so the mock/real switch,
  and any future provider, requires no changes to endpoint code.

### 4.6 Portability
- Backend requires only Python + the packages in `requirements.txt`; no
  OS-specific dependencies. SQLite requires no separate server process.
- Frontend requires only a modern browser; no build step or Node.js
  dependency.

---

## 5. External Interface Requirements

### 5.1 User Interface
Single-page HTML/JS app with three tabs (Requirements, Kanban, AI Chat) and
a project selector in the header. See `prototype/frontend/index.html`.

### 5.2 API (REST, JSON)
| Method | Path | Purpose |
|---|---|---|
| POST | `/projects` | Create project |
| GET | `/projects` | List projects |
| GET | `/projects/{id}` | Get project |
| POST | `/projects/{id}/requirements` | Create requirement |
| GET | `/projects/{id}/requirements` | List requirements |
| POST | `/requirements/{id}/ai/improve` | AI: improve description |
| POST | `/requirements/{id}/ai/user-stories` | AI: generate user stories |
| POST | `/requirements/{id}/ai/acceptance-criteria` | AI: generate acceptance criteria |
| POST | `/projects/{id}/tasks` | Create task |
| GET | `/projects/{id}/tasks` | List tasks |
| PATCH | `/tasks/{id}` | Update task |
| GET | `/assistants` | List available assistants |
| GET | `/projects/{id}/chat/{role}` | Get chat history |
| POST | `/projects/{id}/chat/{role}` | Send chat message, get AI reply |

Full request/response schemas are auto-published at `/docs` (OpenAPI/Swagger)
when the server is running.

### 5.3 AI Provider Interface
`AIProvider.generate(system_prompt: str, user_prompt: str) -> str` — the
only method a new provider implementation must supply.

---

## 6. Data Requirements

Entities: **Project** (name, description), **Requirement** (project_id,
title, description, improved_description, user_stories,
acceptance_criteria), **Task** (project_id, requirement_id?, title,
description, status, priority, labels), **Message** (project_id, role,
sender, content). Full field types are in `prototype/backend/main.py`. No formal ER
diagram or migration history exists yet — see `ARCHITECTURE.md` for the
data model diagram and the migration path to Postgres/Flyway.

---

## 7. Known Limitations (as of this version)
- No authentication — anyone with local access to the running server can
  read/write everything.
- No multi-user support — one implicit user, no ownership model.
- No conversation memory beyond the current requirements/tasks snapshot —
  an assistant does not see its own prior replies as input to a new one
  beyond what's stored for display.
- No retrieval over actual source code — "Developer" and "QA" assistants
  reason only from requirement/task text, not real code.
- SQLite is not safe for concurrent multi-user writes.

---

## 8. Traceability to the Full Product Vision

| Full-vision capability | Status here | Where it's tracked |
|---|---|---|
| JWT auth, RBAC | Not started | README "What to extend first," item 4 |
| Postgres + Flyway | Not started (SQLite instead) | README, item 5 |
| React/TypeScript/MUI frontend | Not started (plain HTML/JS instead) | README, item 6 |
| RAG over source code | Not started | README, item 3 |
| Conversation memory across turns | Not started | README, item 2 |
| Docker/Nginx/CI deployment | Not started | ARCHITECTURE.md §8 |
| Multi-assistant shared context | **Implemented** | FR-4.3 |
| AI provider abstraction | **Implemented** | FR-6.1–6.3 |
| Requirements → AI improvement/user stories/acceptance criteria | **Implemented** | FR-2.3–2.5 |
| Kanban task board | **Implemented** | FR-3.4–3.5 |
