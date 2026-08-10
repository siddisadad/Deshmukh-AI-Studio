# Organization Git Sync Run Filter Presets Guide

**Version:** v0.2.102-beta

Quick-apply preset chips for org-wide git sync run history filters.

## 1. Presets

Settings → **Git** → **Recent sync runs (org-wide)** chip row:

| Preset | Filters applied |
|---|---|
| Failed | status failed |
| Success | status success |
| Manual | source manual |
| Scheduled | source scheduled |
| Webhook | source webhook |
| Failed manual | source manual + status failed |
| Failed scheduled | source scheduled + status failed |

Active preset chip is highlighted. Presets clear project filter when applied (all projects). Overview **View failed runs** still applies project + failed via URL.

Dropdown filters, exports, URL params, and active filter chips use the active preset state.

## 2. Related

- Run filter URL: [105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md](105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md)
- Overview presets: [102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md](102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md)
