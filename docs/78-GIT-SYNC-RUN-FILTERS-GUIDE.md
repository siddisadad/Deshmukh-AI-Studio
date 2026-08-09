# Git Sync Run Filters Guide

**Version:** v0.2.73-beta

Filter and refresh per-project git sync run history in project settings.

## 1. API

`GET /api/v1/projects/{id}/git-link/sync-runs`

| Query param | Default | Values |
|---|---|---|
| `limit` | `20` | 1–100 |
| `source` | (all) | `manual`, `scheduled`, `webhook` |
| `status` | (all) | `success`, `failed` |

Combine filters: `?source=manual&status=success`

## 2. UI

Project settings → Git repository sync → **Recent sync runs**:

- Source filter (all / manual / scheduled / webhook)
- Status filter (all / success / failed)
- **Refresh** button
- Failed runs shown in error color

## 3. Tests

- `ProjectGitLinkControllerIT.listSyncRunsReturnsRecordedManualSync` — source+status filters

## 4. Related

- Run recording: [74-GIT-SYNC-RUN-HISTORY-GUIDE.md](74-GIT-SYNC-RUN-HISTORY-GUIDE.md)
