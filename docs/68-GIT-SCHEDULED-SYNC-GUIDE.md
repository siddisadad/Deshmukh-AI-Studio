# Git scheduled sync cron

**Version:** v0.2.63-beta  
**Scope:** Periodic enqueue of `CODE_METADATA_SYNC` jobs for all enabled project git links.

Complements [64-GIT-CODE-METADATA-SYNC-GUIDE.md](64-GIT-CODE-METADATA-SYNC-GUIDE.md).

---

## Overview

A scheduler runs on a fixed delay and enqueues **full** code metadata sync jobs (`source=scheduled`) for every project with an **enabled** git link that is **due** for sync. Per-project `scheduled_sync_interval_minutes` overrides the platform interval; see [69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md](69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md). Per-project `scheduled_sync_enabled` can disable cron while keeping webhooks/manual sync; see [70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md](70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md). Jobs skip projects that already have a **pending** `CODE_METADATA_SYNC` job.

Webhook delta sync and manual sync paths are unchanged.

---

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `GIT_SYNC_SCHEDULED_ENABLED` | `true` | Enable scheduled git sync scheduler |
| `GIT_SYNC_SCHEDULED_INTERVAL_MS` | `3600000` | Delay between scheduler runs (1 hour) |

Spring properties: `aistudio.git.scheduled-sync-enabled`, `aistudio.git.scheduled-sync-interval-ms`.

---

## Job payload

```json
{ "source": "scheduled" }
```

Worker performs full tree sync (no webhook delta paths).

---

## Tests

- `ProjectGitScheduledSyncIT` — enqueues once; skips duplicate pending job
