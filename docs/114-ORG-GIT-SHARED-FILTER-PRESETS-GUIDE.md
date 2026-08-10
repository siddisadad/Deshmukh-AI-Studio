# Organization Git Shared Filter Presets Guide

**Version:** v0.2.109-beta

Org-wide shared git sync filter presets visible to all organization members.

## 1. API

`visibility` on filter preset create and list responses:

| Value | Meaning |
|---|---|
| `private` | Default — only the creating user sees and manages the preset |
| `org` | Shared with all org members |

### Create body (optional visibility)

```json
{
  "scope": "overview",
  "label": "Org failed GitHub",
  "visibility": "org",
  "filters": { … }
}
```

Only **OWNER** or **ADMIN** may set `visibility: org`.

Limits: max 12 private presets per scope per user; max 12 org-shared presets per scope per organization.

### List response fields

- `visibility` — `private` or `org`
- `createdByUserId`, `createdByDisplayName` — who created the preset
- `count` — match count (see [113-ORG-GIT-SAVED-PRESET-COUNTS-GUIDE.md](113-ORG-GIT-SAVED-PRESET-COUNTS-GUIDE.md))

List returns the current user's private presets plus all org-shared presets.

### Delete rules

- **Private:** creator only
- **Org:** creator, or OWNER/ADMIN

## 2. UI

Settings → **Git** → save preset dialog:

- OWNER/ADMIN see **Share with organization**
- Org-shared chips show `· Org` suffix and info color
- Delete icon hidden for org presets from other members (unless OWNER/ADMIN)

## 3. Related

- Filter presets API: [112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md](112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md)
- Saved preset counts: [113-ORG-GIT-SAVED-PRESET-COUNTS-GUIDE.md](113-ORG-GIT-SAVED-PRESET-COUNTS-GUIDE.md)
