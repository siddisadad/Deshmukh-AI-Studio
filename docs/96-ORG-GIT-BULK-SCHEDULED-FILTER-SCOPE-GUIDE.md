# Organization Git Sync Bulk Scheduled Filter Scope Guide

**Version:** v0.2.91-beta

Scope bulk enable/disable scheduled sync to projects matching the current org git sync overview filters.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/enable-scheduled-sync`

`POST /api/v1/organizations/{orgId}/git-sync-overview/disable-scheduled-sync`

Optional query params (same as overview `GET`):

| Param | Values |
|---|---|
| `linked` | `true`, `false` |
| `enabled` | `true`, `false` |
| `scheduledSyncEnabled` | `true`, `false` |
| `customSyncInterval` | `true`, `false` |
| `provider` | `github`, `gitlab`, `bitbucket` |
| `lastSyncStatus` | `success`, `failed`, `never` |

**Enable** targets linked, enabled, manual-only links that match the filters.

**Disable** targets linked, enabled, scheduled links that match the filters.

With no filters, behavior matches the org-wide bulk actions from Phases 23–24.

## 2. UI

Settings → **Git** → **Git sync overview**:

- Bulk buttons appear when the **filtered** overview has eligible manual-only or scheduled links
- Buttons pass active overview filters to the API
- Label shows **(filtered)** when any overview filter is active
- Success message notes filtered scope when applicable

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanBulkEnableScheduledSyncScopedToOverviewFilters`
- `OrgGitSyncOverviewControllerIT.orgOwnerCanBulkDisableScheduledSyncScopedToOverviewFilters`

## 4. Related

- Bulk enable: [91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md](91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md)
- Bulk disable: [92-ORG-GIT-BULK-DISABLE-SCHEDULED-SYNC-GUIDE.md](92-ORG-GIT-BULK-DISABLE-SCHEDULED-SYNC-GUIDE.md)
- Overview filters: [87-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](87-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md)
