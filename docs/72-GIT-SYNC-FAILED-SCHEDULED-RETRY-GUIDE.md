# Git sync failed scheduled retry

**Version:** v0.2.67-beta  
**Scope:** Scheduled sync re-enqueues failed git links on the next scheduler tick, bypassing per-project interval.

Complements [68-GIT-SCHEDULED-SYNC-GUIDE.md](68-GIT-SCHEDULED-SYNC-GUIDE.md).

---

## Overview

When a git link's `last_sync_status` is `failed`, the scheduled sync scheduler treats the link as **due** immediately (subject to pending-job skip and per-project scheduled toggle). Per-project `scheduled_sync_interval_minutes` and platform interval are bypassed until a successful sync clears the failure.

Retry cadence follows `GIT_SYNC_SCHEDULED_INTERVAL_MS` (platform scheduler tick).

---

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `GIT_SYNC_FAILED_SCHEDULED_RETRY` | `true` | Enable failed-link interval bypass |
| `GIT_SYNC_SCHEDULED_ENABLED` | `true` | Master scheduled sync switch |
| `GIT_SYNC_SCHEDULED_INTERVAL_MS` | `3600000` | Scheduler tick / retry cadence |

Spring property: `aistudio.git.failed-scheduled-retry-enabled`.

---

## Behavior

1. Sync fails → `last_sync_status=failed`, `last_sync_error` set.
2. Next scheduler run enqueues `CODE_METADATA_SYNC` if no pending job exists.
3. Successful sync resets status to `success` and normal interval applies again.

Manual sync and webhooks are unchanged.

---

## Tests

- `ProjectGitScheduledSyncIT.enqueueScheduledSyncsRetriesFailedLinksDespitePerProjectInterval`
