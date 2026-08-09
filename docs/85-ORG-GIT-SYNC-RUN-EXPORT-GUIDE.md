# Organization Git Sync Run Export Guide

**Version:** v0.2.80-beta

Export org-wide git sync run history as CSV or JSON, with the same filters as the list API.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-runs/export`

Requires org membership.

| Param | Description |
|---|---|
| `format` | `csv` (default) or `json` |
| `source` | `manual`, `scheduled`, or `webhook` |
| `status` | `success` or `failed` |
| `projectId` | Optional — restrict to one org project |

Exports up to **1000** matching rows (most recent first). JSON includes `truncated` when more rows exist.

Response is a file download (`Content-Disposition: attachment`).

CSV columns: `id`, `projectId`, `projectName`, `projectKey`, `gitLinkId`, `source`, `status`, `fileCount`, `errorMessage`, `startedAt`, `finishedAt`.

JSON body: `OrgGitSyncRunExportPayload` (`organizationId`, `totalCount`, `exportedCount`, `truncated`, `items`).

## 2. UI

Settings → **Git** → **Recent sync runs (org-wide)**:

- **Export CSV** and **Export JSON** use current source/status filters
- Downloads `git-sync-runs-{orgId}.csv` or `.json`

## 3. Tests

- `OrgGitSyncRunsControllerIT.orgMemberCanExportGitSyncRunsAsCsvAndJson`

## 4. Related

- Org run history: [84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md](84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md)
- Overview export: [83-ORG-GIT-SYNC-OVERVIEW-EXPORT-GUIDE.md](83-ORG-GIT-SYNC-OVERVIEW-EXPORT-GUIDE.md)
