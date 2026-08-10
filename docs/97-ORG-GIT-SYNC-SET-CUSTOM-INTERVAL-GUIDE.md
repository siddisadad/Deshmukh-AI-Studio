# Organization Git Sync Set Custom Interval Guide

**Version:** v0.2.92-beta

Set a per-project scheduled sync interval override from the org git sync overview.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/set-interval-project/{projectId}?scheduledSyncIntervalMinutes={minutes}`

Requires org **OWNER** or **ADMIN**.

Requires an **enabled** git link on the project. `scheduledSyncIntervalMinutes` must be 15–10080.

Returns `OrgGitSyncSetIntervalProjectResponse` (`projectId`, `scheduledSyncIntervalMinutes`, `updated`). `updated=false` when the link already has that interval.

## 2. UI

Settings → **Git** → **Git sync overview** row actions (OWNER/ADMIN, enabled links):

- **Set interval** opens a dialog to enter minutes (15–10080)
- Pre-fills current custom interval when set

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanSetCustomSyncIntervalForSingleProject`

## 4. Related

- Clear interval: [95-ORG-GIT-SYNC-CLEAR-CUSTOM-INTERVAL-GUIDE.md](95-ORG-GIT-SYNC-CLEAR-CUSTOM-INTERVAL-GUIDE.md)
- Interval filter: [94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md](94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md)
- Per-project interval: [69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md](69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md)
