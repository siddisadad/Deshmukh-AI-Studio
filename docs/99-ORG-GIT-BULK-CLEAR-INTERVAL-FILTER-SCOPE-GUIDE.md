# Organization Git Sync Bulk Clear Interval Filter Scope Guide

**Version:** v0.2.94-beta

Scope bulk clear of custom scheduled sync intervals to projects matching the current org git sync overview filters.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/clear-interval`

Optional query params (same as overview `GET`):

| Param | Values |
|---|---|
| `linked` | `true`, `false` |
| `enabled` | `true`, `false` |
| `scheduledSyncEnabled` | `true`, `false` |
| `customSyncInterval` | `true`, `false` |
| `provider` | `github`, `gitlab`, `bitbucket` |
| `lastSyncStatus` | `success`, `failed`, `never` |

Clears `scheduledSyncIntervalMinutes` on linked, enabled git links with a custom interval that match the filters. With no filters, targets all custom-interval links in the org.

Response: `OrgGitSyncClearIntervalResponse` (`targeted`, `updated`, `updatedProjectIds`).

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Clear interval** bulk button when the filtered overview has custom-interval enabled links
- Passes active overview filters to the API
- Label shows **(filtered)** when any overview filter is active

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanBulkClearCustomSyncIntervalScopedToOverviewFilters`

## 4. Related

- Per-project clear: [95-ORG-GIT-SYNC-CLEAR-CUSTOM-INTERVAL-GUIDE.md](95-ORG-GIT-SYNC-CLEAR-CUSTOM-INTERVAL-GUIDE.md)
- Filter-scoped bulk scheduled: [96-ORG-GIT-BULK-SCHEDULED-FILTER-SCOPE-GUIDE.md](96-ORG-GIT-BULK-SCHEDULED-FILTER-SCOPE-GUIDE.md)
- Interval filter: [94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md](94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md)
