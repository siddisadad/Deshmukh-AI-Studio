# Per-project scheduled git sync toggle

**Version:** v0.2.65-beta  
**Scope:** Optional per-project opt-out of scheduled code metadata sync while keeping webhooks and manual sync.

Complements [68-GIT-SCHEDULED-SYNC-GUIDE.md](68-GIT-SCHEDULED-SYNC-GUIDE.md) and [69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md](69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md).

---

## Overview

Each project git link stores `scheduled_sync_enabled` (default `true`). When `false`, the platform scheduler skips the link even if the git link is otherwise **enabled**. Push webhooks and **Sync now** / **Sync in background** are unaffected.

Platform-level `GIT_SYNC_SCHEDULED_ENABLED=false` still disables scheduled sync for all projects.

---

## API

`PUT /api/v1/projects/{id}/git-link`

| Field | Type | Notes |
|-------|------|-------|
| `scheduledSyncEnabled` | boolean | Optional; default `true` on create |

`GET` responses include `scheduledSyncEnabled`.

---

## UI

Project settings → Git repository sync → **Scheduled sync** (yes/no).

---

## Migration

`V39__project_git_scheduled_sync_toggle.sql` — adds `scheduled_sync_enabled BOOLEAN NOT NULL DEFAULT TRUE`.

---

## Tests

- `ProjectGitScheduledSyncIT.enqueueScheduledSyncsSkipsLinksWithScheduledSyncDisabled`
