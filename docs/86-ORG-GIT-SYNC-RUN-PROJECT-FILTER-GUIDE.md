# Organization Git Sync Run Project Filter Guide

**Version:** v0.2.81-beta

Project dropdown on the org-wide git sync run history section, using the existing `projectId` query param.

## 1. API

Already supported on:

- `GET /api/v1/organizations/{orgId}/git-sync-runs?projectId={projectId}`
- `GET /api/v1/organizations/{orgId}/git-sync-runs/export?projectId={projectId}`

`projectId` must belong to the organization.

## 2. UI

Settings → **Git** → **Recent sync runs (org-wide)**:

- **Project** dropdown — all org projects (from sync overview), plus **All projects**
- Combines with source/status filters, refresh, load more, and CSV/JSON export

## 3. Tests

- `OrgGitSyncRunsControllerIT.orgMemberCanListGitSyncRunsAcrossProjects` (API `projectId` filter)

## 4. Related

- Org run history: [84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md](84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md)
- Run export: [85-ORG-GIT-SYNC-RUN-EXPORT-GUIDE.md](85-ORG-GIT-SYNC-RUN-EXPORT-GUIDE.md)
