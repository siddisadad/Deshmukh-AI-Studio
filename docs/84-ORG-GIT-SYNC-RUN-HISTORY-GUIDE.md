# Organization Git Sync Run History Guide

**Version:** v0.2.79-beta

Org-wide git sync run audit trail across all projects, with the same filters and pagination as per-project history.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-runs`

Requires org membership.

| Param | Description |
|---|---|
| `limit` | Page size (default 20, max 100) |
| `offset` | Offset (must be multiple of `limit`) |
| `source` | `manual`, `scheduled`, or `webhook` |
| `status` | `success` or `failed` |
| `projectId` | Optional — restrict to one org project |

Response: `OrgGitSyncRunPageResponse` with `items` including `projectName` and `projectKey`.

## 2. UI

Settings → **Git** → **Recent sync runs (org-wide)**:

- Source/status filters and refresh
- **Project** filter dropdown (all org projects)
- Per-run line shows project key, timestamp, source, status, file count / error
- **Load more** when `hasMore` is true

## 3. Tests

- `OrgGitSyncRunsControllerIT.orgMemberCanListGitSyncRunsAcrossProjects`

## 4. Related

- Per-project runs: [74-GIT-SYNC-RUN-HISTORY-GUIDE.md](74-GIT-SYNC-RUN-HISTORY-GUIDE.md)
- Run filters: [78-GIT-SYNC-RUN-FILTERS-GUIDE.md](78-GIT-SYNC-RUN-FILTERS-GUIDE.md)
- Run pagination: [81-GIT-SYNC-RUN-PAGINATION-GUIDE.md](81-GIT-SYNC-RUN-PAGINATION-GUIDE.md)
