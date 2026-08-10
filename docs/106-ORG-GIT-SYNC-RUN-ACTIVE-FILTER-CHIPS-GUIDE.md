# Organization Git Sync Run Active Filter Chips Guide

**Version:** v0.2.101-beta

Removable chips for each active org-wide git sync run filter dimension.

## 1. UI

Settings → **Git** → **Recent sync runs (org-wide)**:

When any run filter is active, a chip row appears above the filter dropdowns:

| Chip | When shown |
|---|---|
| Source: manual / scheduled / webhook | `runSource` filter |
| Status: success / failed | `runStatus` filter |
| Project: {projectKey} | `runProject` filter |

Click **×** to clear that dimension back to “all” while keeping other run filters. URL query params and exports update accordingly.

## 2. Related

- Run filter URL: [105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md](105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md)
- Overview active chips: [104-ORG-GIT-SYNC-OVERVIEW-ACTIVE-FILTER-CHIPS-GUIDE.md](104-ORG-GIT-SYNC-OVERVIEW-ACTIVE-FILTER-CHIPS-GUIDE.md)
