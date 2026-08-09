# Organization Git Sync Overview Export Guide

**Version:** v0.2.78-beta

Export the org git sync overview dashboard as CSV or JSON, with the same filters as the overview API.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-overview/export`

Requires org membership.

| Param | Description |
|---|---|
| `format` | `csv` (default) or `json` |
| `linked` | Same as overview — `true` / `false` |
| `provider` | `github`, `gitlab`, or `bitbucket` |
| `lastSyncStatus` | `success`, `failed`, or `never` |

Response is a file download (`Content-Disposition: attachment`).

CSV columns: `projectId`, `projectName`, `projectKey`, `linked`, `provider`, `repository`, `branch`, `enabled`, `scheduledSyncEnabled`, `lastSyncedAt`, `lastSyncStatus`, `lastSyncError`, `scheduledSyncIntervalMinutes`.

JSON body matches `OrgGitSyncOverviewResponse` (summary counts + filtered `items`).

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Export CSV** and **Export JSON** use current filter dropdowns
- Downloads `git-sync-overview-{orgId}.csv` or `.json`

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgMemberCanExportGitSyncOverviewAsCsvAndJson`

## 4. Related

- Overview dashboard: [79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md](79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md)
- Overview filters: [80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md)
