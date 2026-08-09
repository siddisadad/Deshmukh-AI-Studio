# Organization Git Sync Overview Enabled Filter Guide

**Version:** v0.2.84-beta

Filter the org git sync overview by git link enabled state, with quick-filter chips on summary counts.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-overview`

| Query param | Default | Values |
|---|---|---|
| `linked` | (all) | `true`, `false` |
| `enabled` | (all) | `true`, `false` |
| `provider` | (all) | `github`, `gitlab`, `bitbucket` |
| `lastSyncStatus` | (all) | `success`, `failed`, `never` |

`enabled=true` returns linked projects with enabled git links only. `enabled=false` returns linked projects with disabled links. Unlinked projects never match an `enabled` filter.

Export endpoint `GET .../git-sync-overview/export` accepts the same filters.

Summary counts (`totalProjects`, `linkedProjects`, `enabledLinks`, `failedLastSync`) reflect the full org; `items` is filtered.

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Enabled** dropdown (all / enabled only / disabled only)
- Clickable summary chips:
  - **linked** count → linked filter
  - **enabled** count → linked + enabled filters
  - **failed last sync** count → last sync failed filter (when count &gt; 0)

## 3. Tests

- `OrgGitSyncOverviewControllerIT.overviewFiltersByEnabledState`

## 4. Related

- Overview filters: [80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md)
- Failed actions: [88-ORG-GIT-SYNC-OVERVIEW-FAILED-ACTIONS-GUIDE.md](88-ORG-GIT-SYNC-OVERVIEW-FAILED-ACTIONS-GUIDE.md)
