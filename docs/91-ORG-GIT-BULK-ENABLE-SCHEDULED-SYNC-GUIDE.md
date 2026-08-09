# Organization Git Sync Bulk Enable Scheduled Sync Guide

**Version:** v0.2.86-beta

Bulk-enable scheduled sync on org projects whose git links are enabled but set to manual only.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/enable-scheduled-sync`

Requires org **OWNER** or **ADMIN**.

Sets `scheduledSyncEnabled=true` on every linked, enabled git link in the organization that currently has scheduled sync disabled.

Response: `OrgGitSyncEnableScheduledResponse` (`targeted`, `updated`, `updatedProjectIds`).

Overview `GET .../git-sync-overview` adds `manualSyncLinks`: count of linked, enabled projects with `scheduledSyncEnabled=false`.

## 2. UI

Settings → **Git** → **Git sync overview**:

- Clickable **manual only** summary count (when &gt; 0) filters to manual-only enabled links
- **Enable scheduled sync** button (OWNER/ADMIN) when `manualSyncLinks` &gt; 0

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanBulkEnableScheduledSync`

## 4. Related

- Scheduled sync filter: [90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md](90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md)
- Bulk retry failed: [82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md](82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md)
