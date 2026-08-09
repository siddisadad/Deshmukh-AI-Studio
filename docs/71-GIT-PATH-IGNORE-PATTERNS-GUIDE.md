# Git path ignore patterns

**Version:** v0.2.66-beta  
**Scope:** Per-project Ant-style glob patterns to exclude paths from git code metadata sync.

Complements [64-GIT-CODE-METADATA-SYNC-GUIDE.md](64-GIT-CODE-METADATA-SYNC-GUIDE.md).

---

## Overview

Each project git link stores `path_ignore_patterns` (JSON array, default empty). Matched paths are skipped for fetch, upsert, and delete. Filter order: **include** (if any) → **ignore**. See [73-GIT-PATH-INCLUDE-PATTERNS-GUIDE.md](73-GIT-PATH-INCLUDE-PATTERNS-GUIDE.md).

Patterns use Spring `AntPathMatcher` semantics (`*`, `**`, `?`). Patterns without `/` are treated as `**/<pattern>` (e.g. `README.md` → `**/README.md`).

---

## API

`PUT /api/v1/projects/{id}/git-link`

| Field | Type | Notes |
|-------|------|-------|
| `pathIgnorePatterns` | string[] | Up to 50 patterns, 200 chars each |
| `clearPathIgnorePatterns` | boolean | Set `true` to clear all patterns |

`GET` responses include `pathIgnorePatterns`.

---

## Examples

| Pattern | Skips |
|---------|-------|
| `README.md` | `README.md` at any depth |
| `**/node_modules/**` | Anything under `node_modules` |
| `*.min.js` | Minified JS bundles |
| `dist/**` | Everything under `dist/` |

---

## UI

Project settings → Git repository sync → **Path ignore patterns** (one pattern per line).

---

## Migration

`V40__project_git_path_ignore_patterns.sql` — `path_ignore_patterns JSONB NOT NULL DEFAULT '[]'`.

---

## Tests

- `GitPathIgnoreMatcherTest`
- `ProjectGitLinkControllerIT.syncNowSkipsIgnoredPathPatterns`
