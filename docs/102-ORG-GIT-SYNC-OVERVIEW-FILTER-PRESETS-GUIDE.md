# Organization Git Sync Overview Filter Presets Guide

**Version:** v0.2.97-beta

Quick-apply combined overview filter presets on the org git sync dashboard.

## 1. Presets

Settings → **Git** → **Git sync overview** chip row:

| Preset | Filters applied |
|---|---|
| Failed (enabled) | linked, enabled, last sync failed |
| Manual only | linked, enabled, manual sync mode |
| Scheduled | linked, enabled, scheduled sync mode |
| Failed scheduled | linked, enabled, scheduled, last sync failed |
| Custom interval | linked, enabled, custom sync interval |
| Never synced | linked, enabled, last sync never |
| Unlinked | unlinked projects only |
| GitHub | linked, enabled, GitHub provider |

Active preset chip is highlighted. Summary count links for scheduled, manual, custom interval, and failed apply the matching preset.

Dropdown filters, bulk actions, and exports use the active preset state.

## 2. API

No new endpoints — presets set the same query params as `GET .../git-sync-overview`.

## 3. Related

- Overview filters: [80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md)
- Bulk actions summary: [101-ORG-GIT-BULK-ACTIONS-SUMMARY-GUIDE.md](101-ORG-GIT-BULK-ACTIONS-SUMMARY-GUIDE.md)
