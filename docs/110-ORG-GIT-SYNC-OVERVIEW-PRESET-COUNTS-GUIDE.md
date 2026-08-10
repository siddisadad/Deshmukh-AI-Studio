# Organization Git Sync Overview Preset Counts Guide

**Version:** v0.2.105-beta

Match counts on org git sync overview filter preset chips.

## 1. UI

Settings → **Git** → **Git sync overview** preset chips show org-wide project counts, e.g. `Manual only (5)`, `Unlinked (2)`.

Counts refresh when overview reloads or bulk/row git sync actions complete.

## 2. API

`GET /api/v1/organizations/{orgId}/git-sync-overview/filter-counts`

Returns preset ids matching built-in overview chips:

| Preset id | Filters applied |
|---|---|
| `failed-enabled` | linked, enabled, last sync failed |
| `manual-enabled` | linked, enabled, manual sync mode |
| `scheduled-enabled` | linked, enabled, scheduled sync mode |
| `failed-scheduled` | linked, enabled, scheduled, last sync failed |
| `custom-interval` | linked, enabled, custom sync interval |
| `never-synced` | linked, enabled, last sync never |
| `unlinked` | unlinked projects |
| `github-enabled` | linked, enabled, GitHub |
| `gitlab-enabled` | linked, enabled, GitLab |
| `bitbucket-enabled` | linked, enabled, Bitbucket |

Response shape: `{ "presets": [{ "id": "manual-enabled", "count": 5 }, ...] }`

Org members only. Counts reflect all projects in the organization.

## 3. Related

- Overview presets: [102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md](102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md)
- Run preset counts: [109-ORG-GIT-SYNC-RUN-PRESET-COUNTS-GUIDE.md](109-ORG-GIT-SYNC-RUN-PRESET-COUNTS-GUIDE.md)
