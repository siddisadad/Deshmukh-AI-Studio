# Organization Git Sync Filter Presets API Guide

**Version:** v0.2.107-beta

Server-synced saved git sync filter presets per user and organization.

## 1. API

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/organizations/{orgId}/git-sync-filter-presets` | List current user's presets |
| POST | `/api/v1/organizations/{orgId}/git-sync-filter-presets` | Create preset |
| DELETE | `/api/v1/organizations/{orgId}/git-sync-filter-presets/{presetId}` | Delete preset |

### Create body

```json
{
  "scope": "overview",
  "label": "My failed GitHub",
  "filters": {
    "linked": "linked",
    "enabled": "enabled",
    "scheduled": "all",
    "interval": "all",
    "provider": "github",
    "status": "failed"
  }
}
```

`scope` is `overview` or `runs`. Overview filters use keys `linked`, `enabled`, `scheduled`, `interval`, `provider`, `status`. Run filters use `source`, `status`, `project`.

Limits: max 12 presets per scope per user. Duplicate filter combinations or labels are rejected.

Org members only. Presets are private to the creating user.

## 2. UI migration

Settings → **Git** saved preset chips now load from the API. On first visit after upgrade, browser `localStorage` presets (Phase 43) are imported once then cleared.

## 3. Related

- Browser-only presets: [111-ORG-GIT-SAVED-FILTER-PRESETS-GUIDE.md](111-ORG-GIT-SAVED-FILTER-PRESETS-GUIDE.md)
- Overview presets: [102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md](102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md)
