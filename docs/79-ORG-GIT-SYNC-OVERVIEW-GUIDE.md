# Organization Git Sync Overview Guide

**Version:** v0.2.74-beta

Org-wide dashboard listing git link and last sync status for every project in the organization.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-overview`

Returns:

| Field | Description |
|---|---|
| `totalProjects` | All org projects |
| `linkedProjects` | Projects with a git link |
| `enabledLinks` | Linked projects where sync is enabled |
| `failedLastSync` | Linked projects whose `lastSyncStatus` is `failed` |
| `items` | Per-project rows (linked and unlinked) |

Each item includes `projectId`, `projectName`, `projectKey`, `linked`, and when linked: provider, repository, branch, enabled flags, `lastSyncedAt`, `lastSyncStatus`, `lastSyncError`, `scheduledSyncIntervalMinutes`.

Requires org membership.

## 2. UI

Settings → **Git** → **Git sync overview** section:

- Summary counts (linked / enabled / failed)
- Per-project status with link to project settings
- **Refresh overview** button

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgMemberCanListGitSyncOverviewAcrossProjects`

## 4. Related

- Per-project sync: [74-GIT-SYNC-RUN-HISTORY-GUIDE.md](74-GIT-SYNC-RUN-HISTORY-GUIDE.md)
- Org credentials: [75-ORG-GIT-CREDENTIALS-GUIDE.md](75-ORG-GIT-CREDENTIALS-GUIDE.md)
- Run filters: [78-GIT-SYNC-RUN-FILTERS-GUIDE.md](78-GIT-SYNC-RUN-FILTERS-GUIDE.md)
