# Git sync run history

**Version:** v0.2.69-beta  
**Scope:** Persist and list per-project git code metadata sync run audit trail.

Complements [64-GIT-CODE-METADATA-SYNC-GUIDE.md](64-GIT-CODE-METADATA-SYNC-GUIDE.md).

---

## Overview

Each full or delta git sync records a row in `project_git_sync_runs` with source (`manual`, `scheduled`, `webhook`), status (`success` / `failed`), file count, error message, and timestamps.

---

## API

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/v1/projects/{id}/git-link/sync-runs` | Optional `limit` (default 20, max 100) |

---

## UI

Project settings → Git repository sync → **Recent sync runs** list.

---

## Migration

`V42__project_git_sync_runs.sql`

---

## Tests

- `ProjectGitLinkControllerIT.listSyncRunsReturnsRecordedManualSync`
