# Coding Standards
## AI Studio for Software Engineering — MVP

Standards for production-ready contributions. Enforce via review + CI linters.

---

## 1. General Principles

1. **Readability > cleverness.**
2. **Small PRs** with one purpose.
3. **No secrets** in git; use env vars.
4. **Fail loudly** with typed errors; never swallow exceptions silently.
5. **AI output is data** — validate/authorize like user input when persisted.

---

## 2. Java / Spring

### Style
- Java 21 features OK (`record`, `sealed`, pattern matching) when clear.
- Package by layer + module (`api`, `application`, `domain`, `infrastructure`).
- Prefer constructor injection; avoid field injection.
- `@Transactional` on application services, not controllers.
- `spring.jpa.open-in-view=false`.

### Naming
| Kind | Convention |
|---|---|
| Classes | `ProjectService`, `ProjectController` |
| Ports | `AiProviderPort`, `ProjectRepositoryPort` |
| DTOs | `CreateProjectRequest`, `ProjectResponse` |
| Exceptions | `ProjectNotFoundException` |

### Nullability & validation
- Validate requests with Bean Validation (`@NotBlank`, `@Email`, `@Size`).
- Use `Optional` only as return type for lookups; avoid `Optional` fields.
- MapStruct for mapping; no duplicated manual mappers without reason.

### Lombok
- Allowed: `@RequiredArgsConstructor`, `@Getter`, `@Builder` on DTOs carefully.
- Avoid `@Data` on JPA entities (equals/hashCode pitfalls). Prefer explicit equals on business key or ID.

### Logging
- SLF4J; use placeholders `log.info("projectId={}", id)`.
- MDC: `requestId`, `userId`.
- Never log passwords, tokens, or full AI prompts in production (truncate/hash).

### Security
- Authorize in application layer for every project-scoped operation.
- Compare resource tenancy before updates.
- Use parameterized queries only (JPA/Criteria).

---

## 3. Database

- All changes via **Flyway**; never rely on `ddl-auto=update` in deployed envs.
- UUID PKs; `TIMESTAMPTZ`.
- Index FKs and filter columns used by APIs.
- Migrations: forward-only; avoid destructive changes without backup plan.

---

## 4. TypeScript / React

### Style
- Strict TypeScript (`strict: true`).
- Function components only.
- Feature folders; shared UI in `shared/components`.
- No `any` unless justified and isolated.

### State
- React Query for server state; Zustand for auth/UI chrome.
- Prefer controlled forms; keep submit handlers thin.

### Components
- Colocate component + light styles; use MUI `sx` sparingly and theme tokens.
- Accessible labels on inputs; buttons need loading/disabled states for async.

### Hooks
- Follow repo React guidance: don’t add `useMemo`/`useCallback` by default unless needed for referential stability with non-compiler setups.
- Custom hooks named `useX` and live near feature.

### Errors
- Central API error type; toast or inline alert consistently.

---

## 5. API Design

- Version prefix `/api/v1`.
- Consistent error envelope.
- Plural nouns; nest by ownership where it aids authz clarity.
- PATCH for partial updates; avoid overloading GET with side effects.
- Document every endpoint in OpenAPI annotations.

---

## 6. AI-Specific Standards

1. Controllers never call vendor SDKs directly.
2. Prompts live in template files with versions.
3. Persist `provider`, `model`, `promptVersion` on messages/actions.
4. Enforce max input lengths.
5. Mark AI fields clearly in API (`improvedDescription`) and UI.
6. Tests must run with Mock provider (no network).

---

## 7. Git & Reviews

### Commits
- Imperative subject: `Add JWT refresh rotation`.
- Group related changes; don’t mix formatting-only with features.

### PR checklist
- [ ] Tests added/updated
- [ ] OpenAPI / docs updated if API changed
- [ ] No new linter errors
- [ ] Authz considered for new endpoints
- [ ] Flyway migration if schema changed

---

## 8. Formatting & Lint

| Area | Tool |
|---|---|
| Java | Spotless or Checkstyle + EditorConfig |
| TS | ESLint + Prettier |
| SQL | Consistent uppercase keywords in migrations |

CI fails on lint errors for changed modules.

---

## 9. Performance Hygiene

- Avoid N+1: use join fetch / entity graphs / DTO queries for lists.
- Paginate all list endpoints.
- Cap AI context size in one place (`ContextBuilder`).

---

## 10. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Baseline coding standards |

**Previous:** `10-SPRINT-PLANNING.md` · **Next:** `12-TESTING-STRATEGY.md`
