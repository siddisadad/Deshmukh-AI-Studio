# Organization Git Sync Clear Custom Interval Guide

**Version:** v0.2.90-beta

Clear a per-project scheduled sync interval override from the org git sync overview.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/clear-interval-project/{projectId}`

Requires org **OWNER** or **ADMIN**.

Requires an **enabled** git link on the project. Returns `OrgGitSyncClearIntervalProjectResponse` (`projectId`, `scheduledSyncIntervalMinutes` null, `updated`). `updated=false` when the link already uses the platform default (null interval).

## 2. UI

Settings → **Git** → **Git sync overview** row actions (OWNER/ADMIN, enabled links with a custom interval):

- **Clear interval** reverts `scheduledSyncIntervalMinutes` to the platform default

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanClearCustomSyncIntervalForSingleProject`

## 4. Related

- Interval filter: [94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md](94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md)
- Per-project interval: [69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md](69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md)
- Scheduled toggle: [93-ORG-GIT-SYNC-PER-PROJECT-SCHEDULED-TOGGLE-GUIDE.md](93-ORG-GIT-SYNC-PER-PROJECT-SCHEDULED-TOGGLE-GUIDE.md)
