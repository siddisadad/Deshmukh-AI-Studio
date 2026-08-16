# Organization Git Sync Update Filter Presets Guide

**Version:** v0.2.115-beta

Overwrite the filter values on an existing saved overview or run preset with the filters currently applied on Settings → **Git**.

## 1. API

`PATCH /api/v1/organizations/{orgId}/git-sync-filter-presets/{presetId}` accepts either or both:

```json
{ "label": "Optional new name" }
```

```json
{
  "filters": {
    "linked": "unlinked",
    "enabled": "all",
    "scheduled": "all",
    "interval": "all",
    "provider": "gitlab",
    "status": "success"
  }
}
```

Rules:

- Same edit permission as rename/delete
- At least one of `label` or `filters` is required
- Duplicate filter combinations within the same scope + visibility are rejected
- Duplicate labels within the same scope + visibility are rejected
- Visibility is unchanged

## 2. UI

When overview/run filters are active and a saved preset is **not** currently matching those filters, an editable preset shows a sync control. Clicking it writes the current filters onto that preset and refreshes chip match counts.

## 3. Related

- Rename presets: [118-ORG-GIT-RENAME-FILTER-PRESETS-GUIDE.md](118-ORG-GIT-RENAME-FILTER-PRESETS-GUIDE.md)
- Filter presets API: [112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md](112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md)
- Saved preset E2E: [117-ORG-GIT-SYNC-SAVED-FILTER-PRESETS-E2E-GUIDE.md](117-ORG-GIT-SYNC-SAVED-FILTER-PRESETS-E2E-GUIDE.md)
