# Organization Git Sync Overview Failed Actions Guide

**Version:** v0.2.83-beta

Per-project retry and navigation for failed git syncs on the org overview dashboard.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/retry-project/{projectId}`

Requires org **OWNER** or **ADMIN**.

Enqueues `CODE_METADATA_SYNC` when the project has an enabled link with `lastSyncStatus=failed`. Skips when a pending sync job already exists.

Response: `OrgGitSyncRetryProjectResponse` (`projectId`, `enqueued`, `skippedPending`).

## 2. UI

Settings → **Git** → **Git sync overview**:

- Clickable **failed last sync** count filters overview to failed status
- Failed project rows (OWNER/ADMIN):
  - **Retry sync** — single-project enqueue
  - **View failed runs** — filters org sync runs to project + failed and scrolls to run history

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanRetryFailedGitSyncForSingleProject`

## 4. Related

- Bulk retry: [82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md](82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md)
- Overview: [79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md](79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md)
