# Organization Git Sync Retry Failed Filter Scope Guide

**Version:** v0.2.93-beta

Scope bulk retry of failed git syncs to projects matching the current org git sync overview filters.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/retry-failed`

Optional query params (same as overview `GET`):

| Param | Values |
|---|---|
| `linked` | `true`, `false` |
| `enabled` | `true`, `false` |
| `scheduledSyncEnabled` | `true`, `false` |
| `customSyncInterval` | `true`, `false` |
| `provider` | `github`, `gitlab`, `bitbucket` |
| `lastSyncStatus` | `success`, `failed`, `never` |

Targets linked, enabled git links with `lastSyncStatus=failed` that match the filters. With no filters, behavior matches the org-wide retry from Phase 14.

Response: `OrgGitSyncRetryFailedResponse` (`targeted`, `enqueued`, `skippedPending`, `enqueuedProjectIds`).

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Retry failed syncs** appears when the filtered overview has failed enabled links
- Passes active overview filters to the API
- Label shows **(filtered)** when any overview filter is active

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanRetryFailedGitSyncsScopedToOverviewFilters`

## 4. Related

- Bulk retry: [82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md](82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md)
- Filter-scoped bulk scheduled: [96-ORG-GIT-BULK-SCHEDULED-FILTER-SCOPE-GUIDE.md](96-ORG-GIT-BULK-SCHEDULED-FILTER-SCOPE-GUIDE.md)
- Overview filters: [87-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](87-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md)
