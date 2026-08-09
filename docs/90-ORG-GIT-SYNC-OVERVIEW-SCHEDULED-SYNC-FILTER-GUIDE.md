# Organization Git Sync Overview Scheduled Sync Filter Guide

**Version:** v0.2.85-beta

Filter the org git sync overview by per-project scheduled sync toggle, with a summary count and quick-filter chip.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-overview`

| Query param | Default | Values |
|---|---|---|
| `linked` | (all) | `true`, `false` |
| `enabled` | (all) | `true`, `false` |
| `scheduledSyncEnabled` | (all) | `true`, `false` |
| `provider` | (all) | `github`, `gitlab`, `bitbucket` |
| `lastSyncStatus` | (all) | `success`, `failed`, `never` |

`scheduledSyncEnabled=true` returns linked projects with scheduled sync enabled. `scheduledSyncEnabled=false` returns linked projects where scheduled sync is off (manual/webhook only). Unlinked projects never match a `scheduledSyncEnabled` filter.

Export endpoint `GET .../git-sync-overview/export` accepts the same filters.

Response adds `scheduledSyncLinks`: count of linked, enabled projects with `scheduledSyncEnabled=true`. Summary counts reflect the full org; `items` is filtered.

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Scheduled sync** dropdown (all / scheduled only / manual only)
- Clickable **scheduled sync** summary chip when count &gt; 0 (sets linked + enabled + scheduled filters)
- Row detail shows `scheduled` vs `manual only`

## 3. Tests

- `OrgGitSyncOverviewControllerIT.overviewFiltersByScheduledSyncEnabled`

## 4. Related

- Enabled filter: [89-ORG-GIT-SYNC-OVERVIEW-ENABLED-FILTER-GUIDE.md](89-ORG-GIT-SYNC-OVERVIEW-ENABLED-FILTER-GUIDE.md)
- Per-project scheduled toggle: [70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md](70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md)
