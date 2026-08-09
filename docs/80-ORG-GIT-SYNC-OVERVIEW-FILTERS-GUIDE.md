# Organization Git Sync Overview Filters Guide

**Version:** v0.2.75-beta

Filter the org git sync overview by link state, provider, and last sync status.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-overview`

| Query param | Default | Values |
|---|---|---|
| `linked` | (all) | `true`, `false` |
| `provider` | (all) | `github`, `gitlab`, `bitbucket` |
| `lastSyncStatus` | (all) | `success`, `failed`, `never` |

Combine filters: `?linked=true&provider=github&lastSyncStatus=success`

Summary counts (`totalProjects`, `linkedProjects`, `enabledLinks`, `failedLastSync`) reflect the full org; `items` is filtered.

## 2. UI

Settings → **Git** → **Git sync overview**:

- Linked filter (all / linked / unlinked)
- Provider filter (all / GitHub / GitLab / Bitbucket)
- Last sync filter (all / success / failed / never)
- **Refresh** button
- Empty state when no rows match

## 3. Tests

- `OrgGitSyncOverviewControllerIT.overviewFiltersByLinkedProviderAndLastSyncStatus`

## 4. Related

- Overview dashboard: [79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md](79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md)
- Per-project run filters: [78-GIT-SYNC-RUN-FILTERS-GUIDE.md](78-GIT-SYNC-RUN-FILTERS-GUIDE.md)
