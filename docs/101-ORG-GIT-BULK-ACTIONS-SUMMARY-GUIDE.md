# Organization Git Sync Bulk Actions Summary Guide

**Version:** v0.2.96-beta

Preview and export how many projects each org git sync bulk action would target under the current overview filters.

## 1. API

`GET /api/v1/organizations/{orgId}/git-sync-overview/bulk-actions-summary`

Optional overview filter query params (same as `GET .../git-sync-overview`).

Requires org **OWNER** or **ADMIN**.

Response: `OrgGitSyncBulkActionsSummaryResponse` with:

- `filteredItems` — projects matching overview filters
- `retryFailedTargeted`, `retryFailedPendingSkipped`
- `enableScheduledTargeted`, `disableScheduledTargeted`
- `clearIntervalTargeted`, `setIntervalTargeted`

Export: `GET .../bulk-actions-summary/export?format=csv|json` with the same filter params.

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Bulk actions preview** line with per-action targeted counts (shows **filtered scope** when filters active)
- **Export summary CSV** / **Export summary JSON** buttons
- **Clear filters** resets all overview dropdowns

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanGetBulkActionsSummaryScopedToOverviewFilters`

## 4. Related

- Overview filters: [80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md)
- Bulk set interval filter scope: [100-ORG-GIT-BULK-SET-INTERVAL-FILTER-SCOPE-GUIDE.md](100-ORG-GIT-BULK-SET-INTERVAL-FILTER-SCOPE-GUIDE.md)
