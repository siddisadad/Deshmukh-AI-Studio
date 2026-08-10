# Organization Git Sync Per-Project Scheduled Toggle Guide

**Version:** v0.2.88-beta

Enable or disable scheduled sync for a single project from the org git sync overview.

## 1. API

`POST /api/v1/organizations/{orgId}/git-sync-overview/enable-scheduled-project/{projectId}`

`POST /api/v1/organizations/{orgId}/git-sync-overview/disable-scheduled-project/{projectId}`

Requires org **OWNER** or **ADMIN**.

Requires an **enabled** git link on the project. Returns `OrgGitSyncScheduledProjectResponse` (`projectId`, `scheduledSyncEnabled`, `updated`). `updated=false` when the link already matches the requested state.

## 2. UI

Settings → **Git** → **Git sync overview** row actions (OWNER/ADMIN, enabled links):

- **Enable scheduled** when `scheduledSyncEnabled=false`
- **Disable scheduled** when `scheduledSyncEnabled=true`

## 3. Tests

- `OrgGitSyncOverviewControllerIT.orgOwnerCanToggleScheduledSyncForSingleProject`

## 4. Related

- Bulk enable: [91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md](91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md)
- Bulk disable: [92-ORG-GIT-BULK-DISABLE-SCHEDULED-SYNC-GUIDE.md](92-ORG-GIT-BULK-DISABLE-SCHEDULED-SYNC-GUIDE.md)
- Per-project retry: [88-ORG-GIT-SYNC-OVERVIEW-FAILED-ACTIONS-GUIDE.md](88-ORG-GIT-SYNC-OVERVIEW-FAILED-ACTIONS-GUIDE.md)
