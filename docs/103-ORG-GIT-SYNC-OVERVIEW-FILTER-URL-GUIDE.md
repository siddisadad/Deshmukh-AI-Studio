# Organization Git Sync Overview Filter URL Guide

**Version:** v0.2.98-beta

Bookmark and share org git sync overview filter state via URL query parameters.

## 1. URL parameters

Settings → **Git** at `/settings/git` syncs active overview filters to the query string:

| Param | Values | Maps to overview filter |
|---|---|---|
| `linked` | `linked`, `unlinked` | Linked dropdown |
| `enabled` | `enabled`, `disabled` | Enabled dropdown |
| `scheduled` | `scheduled`, `manual` | Scheduled sync dropdown |
| `interval` | `custom`, `default` | Sync interval dropdown |
| `provider` | `github`, `gitlab`, `bitbucket` | Provider dropdown |
| `lastSync` | `success`, `failed`, `never` | Last sync dropdown |

Omitted params mean “all” for that dimension. Preset chips and dropdowns update the URL; opening a link with these params applies the filters on load.

Example:

`/settings/git?linked=linked&enabled=enabled&lastSync=failed`

## 2. UI

- **Copy filtered link** — copies the current page URL when any overview filter is active
- **Clear filters** removes overview params from the URL

## 3. Related

- Filter presets: [102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md](102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md)
- Overview filters: [80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md)
