# Organization Git Sync Retry Failed Guide

**Version:** v0.2.77-beta

Enqueue background code metadata sync for every enabled project git link whose last sync failed.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/retry-failed`

Requires org **OWNER** or **ADMIN**.

Response:

| Field | Description |
|---|---|
| `targeted` | Enabled links with `lastSyncStatus=failed` |
| `enqueued` | Background jobs created |
| `skippedPending` | Skipped because a `CODE_METADATA_SYNC` job is already pending |
| `enqueuedProjectIds` | Project IDs queued |

Jobs use `source=manual` and run via the standard background worker.

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Retry failed syncs** button when `failedLastSync > 0` (owners only)
- Shows enqueue result in the success alert

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanRetryFailedGitSyncs`

## 4. Related

- Overview dashboard: [79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md](79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md)
- Failed scheduled retry: [72-GIT-SYNC-FAILED-SCHEDULED-RETRY-GUIDE.md](72-GIT-SYNC-FAILED-SCHEDULED-RETRY-GUIDE.md)
