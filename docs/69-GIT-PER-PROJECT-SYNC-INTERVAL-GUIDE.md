# Per-project git sync interval

**Version:** v0.2.64-beta  
**Scope:** Optional per-project override for scheduled code metadata sync cadence.

Complements [68-GIT-SCHEDULED-SYNC-GUIDE.md](68-GIT-SCHEDULED-SYNC-GUIDE.md).

---

## Overview

Each project git link can store `scheduled_sync_interval_minutes` (nullable). When set, the scheduled sync scheduler enqueues a job only after that many minutes have elapsed since `last_synced_at`. When null, the platform default from `GIT_SYNC_SCHEDULED_INTERVAL_MS` applies.

Manual sync and webhook paths ignore this interval.

---

## API

`PUT /api/v1/projects/{id}/git-link`

| Field | Type | Notes |
|-------|------|-------|
| `scheduledSyncIntervalMinutes` | integer | Optional override, 15–10080 (7 days) |
| `clearScheduledSyncInterval` | boolean | Set `true` to revert to platform default |

`GET` responses include `scheduledSyncIntervalMinutes` (null = platform default).

---

## Scheduler behavior

1. Scheduler runs on `GIT_SYNC_SCHEDULED_INTERVAL_MS` (global tick).
2. For each enabled link: skip if `last_synced_at` is within the effective interval.
3. Skip if a pending `CODE_METADATA_SYNC` job already exists for the project.
4. Enqueue `{ "source": "scheduled" }` when due.

---

## UI

Project settings → Git repository sync → **Scheduled sync interval (minutes)**. Leave blank for platform default.

---

## Migration

`V38__project_git_sync_interval.sql` — adds `scheduled_sync_interval_minutes` to `project_git_links`.

---

## Tests

- `ProjectGitScheduledSyncIT.enqueueScheduledSyncsSkipsLinksNotDueForPerProjectInterval`
