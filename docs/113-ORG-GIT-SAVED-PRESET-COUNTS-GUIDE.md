# Organization Git Saved Preset Counts Guide

**Version:** v0.2.108-beta

Match counts on server-synced saved git sync filter preset chips.

## 1. UI

Settings → **Git** saved preset chips (overview and run sections) show match counts, e.g. `My failed GitHub (2)`.

Counts refresh when overview or run history is reloaded (filter change, refresh, pagination).

## 2. API

`GET /api/v1/organizations/{orgId}/git-sync-filter-presets` includes a `count` field on each preset:

```json
[
  {
    "id": "…",
    "scope": "overview",
    "label": "My failed GitHub",
    "filters": { "linked": "linked", "enabled": "enabled", … },
    "count": 2,
    "createdAt": "…"
  }
]
```

Overview counts match projects in the org git sync overview. Run counts match sync runs across org projects (or a single project when the preset filters by project).

Org members only. Counts are computed for the current user's saved presets.

## 3. Related

- Filter presets API: [112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md](112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md)
- Built-in overview counts: [110-ORG-GIT-SYNC-OVERVIEW-PRESET-COUNTS-GUIDE.md](110-ORG-GIT-SYNC-OVERVIEW-PRESET-COUNTS-GUIDE.md)
- Built-in run counts: [109-ORG-GIT-SYNC-RUN-PRESET-COUNTS-GUIDE.md](109-ORG-GIT-SYNC-RUN-PRESET-COUNTS-GUIDE.md)
