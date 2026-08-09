# Git path include patterns

**Version:** v0.2.68-beta  
**Scope:** Per-project Ant-style glob patterns to limit which repository paths are synced.

Complements [71-GIT-PATH-IGNORE-PATTERNS-GUIDE.md](71-GIT-PATH-IGNORE-PATTERNS-GUIDE.md).

---

## Overview

Each project git link stores `path_include_patterns` (JSON array, default empty). When empty, all code-like paths are eligible (subject to ignore patterns). When set, only paths matching at least one include pattern are synced.

Filter order: **include** (if any) → **ignore**.

Patterns use the same Ant-style semantics as ignore patterns (`GitPathIgnoreMatcher`).

---

## API

`PUT /api/v1/projects/{id}/git-link`

| Field | Type | Notes |
|-------|------|-------|
| `pathIncludePatterns` | string[] | Up to 50 patterns, 200 chars each |
| `clearPathIncludePatterns` | boolean | Set `true` to clear (sync all paths) |

`GET` responses include `pathIncludePatterns`.

---

## Examples

| Pattern | Includes |
|---------|----------|
| `src/**` | Everything under `src/` |
| `**/*.java` | Java files at any depth |
| `docs/**/*.md` | Markdown under `docs/` |

---

## UI

Project settings → Git repository sync → **Path include patterns** (one per line).

---

## Migration

`V41__project_git_path_include_patterns.sql` — `path_include_patterns JSONB NOT NULL DEFAULT '[]'`.

---

## Tests

- `GitPathIgnoreMatcherTest.includePatternsLimitScopeWhenSet`
- `ProjectGitLinkControllerIT.syncNowAppliesPathIncludePatterns`
