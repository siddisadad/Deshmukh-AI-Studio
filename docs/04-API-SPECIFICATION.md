# API Specification
## AI Studio for Software Engineering — MVP

| Field | Value |
|---|---|
| **Style** | REST, JSON |
| **Base URL** | `/api/v1` |
| **Auth** | `Authorization: Bearer <access_token>` |
| **Docs** | OpenAPI 3.1 via springdoc (`/v3/api-docs`, `/swagger-ui`) |

---

## 1. Conventions

### 1.1 HTTP Status Codes
| Code | When |
|---|---|
| 200 | Successful read/update |
| 201 | Resource created |
| 204 | Successful delete / no body |
| 400 | Validation / malformed request |
| 401 | Missing/invalid auth |
| 403 | Authenticated but not allowed |
| 404 | Resource not found (or hidden by authz) |
| 409 | Conflict (duplicate email, key) |
| 429 | Rate limited |
| 500 | Unexpected server error |
| 502/503 | Upstream AI provider failure |

### 1.2 Error Body
```json
{
  "timestamp": "2026-08-06T22:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/projects",
  "requestId": "b3a1...",
  "details": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

### 1.3 Pagination
Query: `page` (0-based), `size` (default 20, max 100), `sort` optional.  
Response wrapper:
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### 1.4 IDs & Time
UUIDs as strings; timestamps ISO-8601 UTC.

---

## 2. Authentication

### POST `/api/v1/auth/register`
Create user + personal organization.

**Request**
```json
{
  "email": "ada@example.com",
  "password": "Str0ngPass!",
  "displayName": "Ada Lovelace"
}
```
**Validation:** email valid; password ≥ 10 chars with complexity; displayName 1–120.

**Response `201`**
```json
{
  "user": {
    "id": "...",
    "email": "ada@example.com",
    "displayName": "Ada Lovelace",
    "theme": "SYSTEM"
  },
  "organization": { "id": "...", "name": "Ada's Workspace", "slug": "ada-s-workspace" },
  "accessToken": "<jwt>",
  "refreshToken": "<opaque>",
  "expiresIn": 900
}
```

### POST `/api/v1/auth/login`
**Request:** `{ "email", "password" }` → `200` same token shape as register.  
**Errors:** `401 INVALID_CREDENTIALS` (generic message).

### POST `/api/v1/auth/refresh`
**Request:** `{ "refreshToken" }` → new access (+ rotated refresh).  
**Errors:** `401` if revoked/expired.

### POST `/api/v1/auth/logout`
**Auth required.** Revokes refresh token. `204`.

### POST `/api/v1/auth/forgot-password`
**Request:** `{ "email" }` → always `202` (no email enumeration).  
Creates reset token; sends email (or logs in dev).

### POST `/api/v1/auth/reset-password`
**Request:** `{ "token", "newPassword" }` → `204`.  
**Errors:** `400 TOKEN_INVALID_OR_EXPIRED`.

---

## 3. Profile

### GET `/api/v1/me`
**Auth.** Returns current user profile + default org.

### PATCH `/api/v1/me`
**Request**
```json
{ "displayName": "Ada", "theme": "DARK" }
```

### POST `/api/v1/me/password`
**Request:** `{ "currentPassword", "newPassword" }` → `204`.

---

## 4. Organizations

### GET `/api/v1/organizations`
List orgs for current user.

### GET `/api/v1/organizations/{orgId}`
Org detail + membership role.

### GET `/api/v1/organizations/{orgId}/members`
List members (ADMIN+).

### POST `/api/v1/organizations/{orgId}/members`
**Request:** `{ "email", "role" }` — invite/add existing user (MVP).  
**Authz:** OWNER/ADMIN.

---

## 5. Projects

### POST `/api/v1/organizations/{orgId}/projects`
**Request**
```json
{
  "name": "Client Portal",
  "projectKey": "CP",
  "description": "Customer-facing portal"
}
```
**Validation:** name required; projectKey `^[A-Z][A-Z0-9]{1,9}$`.  
**Response `201`:** project DTO; creator added as project OWNER.

### GET `/api/v1/organizations/{orgId}/projects`
Query: `status=ACTIVE|ARCHIVED|ALL`. Paginated.

### GET `/api/v1/projects/{projectId}`
Project detail if member.

### PATCH `/api/v1/projects/{projectId}`
Update name/description/key (key change restricted).

### POST `/api/v1/projects/{projectId}/archive`
Sets `ARCHIVED` + `archived_at`. ADMIN+. `200`.

### POST `/api/v1/projects/{projectId}/unarchive`
Restore to ACTIVE. ADMIN+.

### GET `/api/v1/dashboard`
Aggregates across user's active projects:
```json
{
  "projects": [
    {
      "id": "...",
      "name": "Client Portal",
      "requirementCount": 12,
      "openTaskCount": 8,
      "doneTaskCount": 4,
      "updatedAt": "..."
    }
  ],
  "recentActivity": []
}
```

---

## 6. Requirements

### POST `/api/v1/projects/{projectId}/requirements`
**Request**
```json
{
  "title": "User can reset password",
  "description": "As a user...",
  "priority": "HIGH",
  "status": "DRAFT"
}
```

### GET `/api/v1/projects/{projectId}/requirements`
Paginated list.

### GET `/api/v1/requirements/{requirementId}`
### PATCH `/api/v1/requirements/{requirementId}`
### DELETE `/api/v1/requirements/{requirementId}` → `204`

### AI Actions
All require MEMBER+; rate-limited.

#### POST `/api/v1/requirements/{requirementId}/ai/improve`
Optional body: `{ "instructions": "Focus on security" }`.  
Persists `improvedDescription`; returns requirement + generation metadata.

#### POST `/api/v1/requirements/{requirementId}/ai/user-stories`
Persists `userStories` (markdown).

#### POST `/api/v1/requirements/{requirementId}/ai/acceptance-criteria`
Persists `acceptanceCriteria` (markdown).

**AI response envelope (shared)**
```json
{
  "requirement": { "...": "..." },
  "assistantRole": "BUSINESS_ANALYST",
  "provider": "mock",
  "model": "mock-1",
  "generatedText": "..."
}
```

---

## 7. Tasks & Labels

### Labels
- `POST /api/v1/projects/{projectId}/labels` `{ "name", "color" }`
- `GET /api/v1/projects/{projectId}/labels`
- `DELETE /api/v1/labels/{labelId}`

### Tasks
#### POST `/api/v1/projects/{projectId}/tasks`
```json
{
  "title": "Implement reset endpoint",
  "description": "...",
  "priority": "HIGH",
  "status": "TODO",
  "requirementId": "...",
  "assigneeId": "...",
  "labelIds": ["..."]
}
```

#### GET `/api/v1/projects/{projectId}/tasks`
Query filters: `status`, `priority`, `assigneeId`.

#### GET `/api/v1/tasks/{taskId}`
#### PATCH `/api/v1/tasks/{taskId}` — partial update including status for Kanban moves
#### DELETE `/api/v1/tasks/{taskId}` → `204`

#### PATCH `/api/v1/projects/{projectId}/tasks/reorder`
```json
{ "updates": [ { "taskId": "...", "status": "IN_PROGRESS", "sortOrder": 2 } ] }
```

---

## 8. Documents

### POST `/api/v1/projects/{projectId}/documents`
```json
{ "title": "README", "docType": "README", "contentMd": "# ..." }
```

### GET `/api/v1/projects/{projectId}/documents`
### GET `/api/v1/documents/{documentId}`
### PATCH `/api/v1/documents/{documentId}`
### DELETE `/api/v1/documents/{documentId}`

### POST `/api/v1/documents/{documentId}/ai/generate`
```json
{ "instructions": "Target junior developers" }
```
Uses Documentation Writer + shared context; updates `contentMd`.

---

## 9. Context Assets

### PUT `/api/v1/projects/{projectId}/context-assets/{assetType}`
Upsert DB design / API spec / source metadata.
```json
{ "title": "ERD notes", "content": "...", "metadata": {} }
```
`assetType`: `DATABASE_DESIGN` | `API_SPEC` | `SOURCE_METADATA` | `OTHER`

### GET `/api/v1/projects/{projectId}/context-assets`

---

## 10. AI Assistants & Chat

### GET `/api/v1/assistants`
```json
{
  "assistants": [
    {
      "role": "BUSINESS_ANALYST",
      "name": "Business Analyst",
      "capabilities": ["improve_requirements", "user_stories", "acceptance_criteria"],
      "limitations": ["Does not write production code"]
    }
  ]
}
```

### GET `/api/v1/projects/{projectId}/conversations/{assistantRole}`
Returns conversation + messages (paginated messages via `?page&size`).

### POST `/api/v1/projects/{projectId}/conversations/{assistantRole}/messages`
```json
{ "content": "Suggest a REST API for password reset" }
```
**Response `200`**
```json
{
  "userMessage": { "id": "...", "sender": "USER", "content": "...", "createdAt": "..." },
  "assistantMessage": { "id": "...", "sender": "ASSISTANT", "content": "...", "createdAt": "..." },
  "provider": "anthropic",
  "model": "claude-..."
}
```

### Developer / QA specialized actions (optional shortcuts)
| Method | Path | Assistant |
|---|---|---|
| POST | `/api/v1/projects/{projectId}/ai/developer/api-suggestions` | Developer |
| POST | `/api/v1/projects/{projectId}/ai/developer/db-suggestions` | Developer |
| POST | `/api/v1/projects/{projectId}/ai/developer/code-review` | Developer |
| POST | `/api/v1/projects/{projectId}/ai/qa/test-cases` | QA |
| POST | `/api/v1/projects/{projectId}/ai/qa/api-scenarios` | QA |
| POST | `/api/v1/projects/{projectId}/ai/qa/bug-report` | QA |
| POST | `/api/v1/projects/{projectId}/ai/qa/regression-checklist` | QA |
| POST | `/api/v1/projects/{projectId}/ai/docs/readme` | Docs |
| POST | `/api/v1/projects/{projectId}/ai/docs/api` | Docs |
| POST | `/api/v1/projects/{projectId}/ai/docs/release-notes` | Docs |

Bodies accept optional `{ "focus", "input", "documentId" }` as relevant. Chat remains the general interface; shortcuts are convenience wrappers over the same `AiService`.

---

## 11. Health & Meta

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | `/actuator/health` | No | Liveness/readiness |
| GET | `/actuator/info` | No | Build info |
| GET | `/v3/api-docs` | No/secured in prod | OpenAPI JSON |
| GET | `/swagger-ui/index.html` | Dev | Interactive docs |

---

## 12. Rate Limiting

| Bucket | Limit (MVP defaults) |
|---|---|
| Auth login per IP | 10 / minute |
| AI endpoints per user | 30 / minute |
| General API per user | 300 / minute |

Response headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After` on 429.

---

## 13. OpenAPI Generation Checklist

- springdoc-openapi configured with security scheme `bearerAuth`.
- All DTOs annotated with `@Schema` / validation annotations.
- Error responses documented with shared `ApiError` schema.
- Examples for register, create project, AI improve, chat message.

Export artifact for CI: `openapi.json` committed or generated in build (`./gradlew generateOpenApiDocs` or springdoc maven plugin).

---

## 14. Security Notes for Clients

- Store access token in memory; refresh token in httpOnly cookie (preferred) or secure storage.
- Do not log Authorization headers.
- Treat 403/404 uniformly in UI for unauthorized project access (avoid IDOR leaks).

---

## 15. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Full MVP REST surface |

**Previous:** `03-DATABASE-DESIGN.md` · **Next:** `05-BACKEND-STRUCTURE.md`
