# Organization Git Sync Full Page Filter State Guide

**Version:** v0.2.103-beta

Combined overview and sync-run filter state on the org Git credentials page.

## 1. Page toolbar

When any overview or run history filter is active, Settings → **Git** shows a toolbar above **Git sync overview**:

| Control | Behavior |
|---|---|
| Active filter count | Total active dimensions across overview (`linked`, `enabled`, `scheduled`, `interval`, `provider`, `lastSync`) and runs (`runSource`, `runStatus`, `runProject`) |
| **Copy page link** | Copies a URL with all active overview and run query params; adds `#org-git-sync-runs` when run filters are set |
| **Clear all filters** | Resets overview and run filters in one action |

Section-level **Copy filtered link** and **Clear filters** remain for overview-only or runs-only scope.

## 2. URL params

Overview keys: `linked`, `enabled`, `scheduled`, `interval`, `provider`, `lastSync`

Run keys: `runSource`, `runStatus`, `runProject`

Opening a full page link applies overview filters on load; run filters load run history and scroll to the runs section when present.

## 3. Related

- Overview URL: [103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md](103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md)
- Run URL: [105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md](105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md)
