# Organization Git Sync Bulk Set Interval Filter Scope Guide

**Version:** v0.2.95-beta

Scope bulk set of custom scheduled sync intervals to projects matching the current org git sync overview filters.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/set-interval?scheduledSyncIntervalMinutes={minutes}`

Optional overview filter query params (same as `GET .../git-sync-overview`).

Requires org **OWNER** or **ADMIN**. `scheduledSyncIntervalMinutes` must be 15–10080.

Sets the interval on linked, enabled git links that match the filters. Skips links that already have that interval.

Response: `OrgGitSyncSetIntervalResponse` (`targeted`, `updated`, `updatedProjectIds`).

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Set interval** bulk button when the filtered overview has enabled linked projects
- Opens a dialog to enter minutes; passes active overview filters to the API
- Label shows **(filtered)** when any overview filter is active

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanBulkSetCustomSyncIntervalScopedToOverviewFilters`

## 4. Related

- Per-project set: [97-ORG-GIT-SYNC-SET-CUSTOM-INTERVAL-GUIDE.md](97-ORG-GIT-SYNC-SET-CUSTOM-INTERVAL-GUIDE.md)
- Bulk clear interval: [99-ORG-GIT-BULK-CLEAR-INTERVAL-FILTER-SCOPE-GUIDE.md](99-ORG-GIT-BULK-CLEAR-INTERVAL-FILTER-SCOPE-GUIDE.md)
- Interval filter: [94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md](94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md)
