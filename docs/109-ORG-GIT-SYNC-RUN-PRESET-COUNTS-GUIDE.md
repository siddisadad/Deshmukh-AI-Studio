# Organization Git Sync Run Preset Counts Guide

**Version:** v0.2.104-beta

Match counts on org-wide git sync run filter preset chips.

## 1. UI

Settings → **Git** → **Recent sync runs (org-wide)** preset chips show org-wide counts, e.g. `Failed (3)`, `Manual (12)`.

Counts refresh when run history is reloaded (filter change, refresh, pagination load).

## 2. API

`GET /api/v1/organizations/{orgId}/git-sync-runs/filter-counts`

Returns preset ids matching built-in chips:

| Preset id | Filters |
|---|---|
| `failed` | status failed |
| `success` | status success |
| `manual` | source manual |
| `scheduled` | source scheduled |
| `webhook` | source webhook |
| `failed-manual` | source manual + status failed |
| `failed-scheduled` | source scheduled + status failed |

Response shape: `{ "presets": [{ "id": "failed", "count": 3 }, ...] }`

Org members only. Counts span all projects in the organization (project filter not applied).

## 3. Related

- Run presets: [107-ORG-GIT-SYNC-RUN-FILTER-PRESETS-GUIDE.md](107-ORG-GIT-SYNC-RUN-FILTER-PRESETS-GUIDE.md)
- Run list API: org `git-sync-runs` endpoint
