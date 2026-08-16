# Organization Git Sync Rename Filter Presets Guide

**Version:** v0.2.114-beta

Rename saved overview and run filter presets on Settings → **Git** without changing their filters.

## 1. API

| Method | Path | Description |
|---|---|---|
| PATCH | `/api/v1/organizations/{orgId}/git-sync-filter-presets/{presetId}` | Rename preset |

### Body

```json
{ "label": "My renamed filters" }
```

Rules:

- Same edit permission as delete (private: creator; org-shared: creator or OWNER/ADMIN)
- Duplicate labels within the same scope + visibility are rejected
- Filters and visibility are unchanged
- Label max length 40 (trimmed)

## 2. UI

Saved preset chips show an edit (pencil) control when the current user can rename. Opening it shows **Rename … filter preset**; confirm updates the chip label and shows a success message.

## 3. Related

- Filter presets API: [112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md](112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md)
- Org-shared presets: [114-ORG-GIT-SHARED-FILTER-PRESETS-GUIDE.md](114-ORG-GIT-SHARED-FILTER-PRESETS-GUIDE.md)
- Saved preset E2E: [117-ORG-GIT-SYNC-SAVED-FILTER-PRESETS-E2E-GUIDE.md](117-ORG-GIT-SYNC-SAVED-FILTER-PRESETS-E2E-GUIDE.md)
