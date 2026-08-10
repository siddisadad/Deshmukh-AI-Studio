# Organization Git Sync Overview Custom Interval Filter Guide

**Version:** v0.2.89-beta

Filter the org git sync overview by per-project scheduled sync interval override.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-overview`

| Query param | Default | Values |
|---|---|---|
| `customSyncInterval` | (all) | `true`, `false` |

`customSyncInterval=true` returns linked projects with a non-null `scheduledSyncIntervalMinutes`. `customSyncInterval=false` returns linked projects using the platform default (null interval). Unlinked projects never match.

Export endpoint accepts the same filter.

Response adds `customSyncIntervalLinks`: count of linked, enabled projects with a custom interval set.

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Sync interval** dropdown (all / custom interval / platform default)
- Clickable **custom interval** summary chip when count &gt; 0
- Row detail shows `interval Nm` when a custom interval is set

## 3. Tests

- `OrgGitSyncOverviewControllerIT.overviewFiltersByCustomSyncInterval`

## 4. Related

- Per-project interval: [69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md](69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md)
- Scheduled sync filter: [90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md](90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md)
