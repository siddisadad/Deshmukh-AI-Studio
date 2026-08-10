# Organization Git Saved Filter Presets Guide

**Version:** v0.2.106-beta

Save custom overview and sync-run filter combinations as named presets in the browser.

## 1. Overview saved presets

Settings → **Git** → **Git sync overview**:

- **Save filters** appears when any overview filter is active
- Enter a name (max 40 characters) in the dialog
- Saved presets appear as secondary chips below built-in presets
- Click to apply; delete icon removes from this browser
- Duplicate filter combinations are rejected

## 2. Run saved presets

**Recent sync runs (org-wide)** section mirrors overview:

- **Save filters** when run filters are active
- Saved run presets apply source, status, and project filters

## 3. Storage

- Browser `localStorage` keyed per organization
- Keys: `aistudio.org-git-sync.saved-overview-presets.{orgId}` and `aistudio.org-git-sync.saved-run-presets.{orgId}`
- Max 12 saved presets per section per org
- Not synced across devices or users — use **Copy page link** for shareable URLs

## 4. Related

- Overview presets: [102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md](102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md)
- Run presets: [107-ORG-GIT-SYNC-RUN-FILTER-PRESETS-GUIDE.md](107-ORG-GIT-SYNC-RUN-FILTER-PRESETS-GUIDE.md)
- Full page link: [108-ORG-GIT-SYNC-FULL-PAGE-FILTER-STATE-GUIDE.md](108-ORG-GIT-SYNC-FULL-PAGE-FILTER-STATE-GUIDE.md)
