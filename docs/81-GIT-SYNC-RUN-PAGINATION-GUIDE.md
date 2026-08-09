# Git Sync Run Pagination Guide

**Version:** v0.2.76-beta

Offset-based pagination for per-project git sync run history.

## 1. API

`GET /api/v1/projects/{id}/git-link/sync-runs`

| Query param | Default | Notes |
|---|---|---|
| `limit` | `20` | 1–100 |
| `offset` | `0` | Must be a multiple of `limit` |
| `source` | (all) | `manual`, `scheduled`, `webhook` |
| `status` | (all) | `success`, `failed` |

Response (`GitSyncRunPageResponse`):

| Field | Description |
|---|---|
| `items` | Sync runs for this page |
| `offset` | Applied offset |
| `limit` | Applied limit |
| `totalCount` | Total runs matching filters |
| `hasMore` | More pages available |

Example: `?limit=2&offset=0` then `?limit=2&offset=2`

## 2. UI

Project settings → Git repository sync → **Recent sync runs**:

- Shows “Showing N of total”
- **Load more** when `hasMore` is true
- Filter/refresh resets to offset 0

## 3. Tests

- `ProjectGitLinkControllerIT.listSyncRunsReturnsRecordedManualSync` — page wrapper + filters
- `ProjectGitLinkControllerIT.listSyncRunsSupportsOffsetPagination`

## 4. Related

- Run history: [74-GIT-SYNC-RUN-HISTORY-GUIDE.md](74-GIT-SYNC-RUN-HISTORY-GUIDE.md)
- Run filters: [78-GIT-SYNC-RUN-FILTERS-GUIDE.md](78-GIT-SYNC-RUN-FILTERS-GUIDE.md)
