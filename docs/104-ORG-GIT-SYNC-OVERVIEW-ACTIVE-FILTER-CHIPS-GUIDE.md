# Organization Git Sync Overview Active Filter Chips Guide

**Version:** v0.2.99-beta

Removable chips for each active org git sync overview filter dimension.

## 1. UI

Settings → **Git** → **Git sync overview**:

When any overview filter is active, an **Active filters** chip row appears below the preset chips. Each chip shows one applied dimension:

| Chip | When shown |
|---|---|
| Linked only / Unlinked only | `linked` filter |
| Enabled only / Disabled only | `enabled` filter |
| Scheduled only / Manual only | `scheduled` filter |
| Custom interval / Platform default | `interval` filter |
| `github` / `gitlab` / `bitbucket` | `provider` filter |
| Last sync: success/failed/never | `lastSync` filter |

Click the chip **×** to clear that dimension back to “all” while keeping other filters. URL query params and bulk action scope update accordingly.

## 2. Related

- Filter presets: [102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md](102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md)
- Filter URL sharing: [103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md](103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md)
