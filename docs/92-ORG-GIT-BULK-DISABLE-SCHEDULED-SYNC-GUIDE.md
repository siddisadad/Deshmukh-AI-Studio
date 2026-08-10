# Organization Git Sync Bulk Disable Scheduled Sync Guide

**Version:** v0.2.87-beta

Bulk-disable scheduled sync on org projects whose enabled git links have scheduled sync turned on.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/disable-scheduled-sync`

Requires org **OWNER** or **ADMIN**.

Sets `scheduledSyncEnabled=false` on every linked, enabled git link in the organization that currently has scheduled sync enabled.

Response: `OrgGitSyncDisableScheduledResponse` (`targeted`, `updated`, `updatedProjectIds`).

## 2. UI

Settings → **Git** → **Git sync overview**:

- **Disable scheduled sync** button (OWNER/ADMIN) when `scheduledSyncLinks` &gt; 0

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanBulkDisableScheduledSync`

## 4. Related

- Bulk enable: [91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md](91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md)
- Scheduled sync filter: [90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md](90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md)
