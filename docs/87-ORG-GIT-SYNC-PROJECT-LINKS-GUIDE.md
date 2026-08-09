# Organization Git Sync Project Settings Links Guide

**Version:** v0.2.82-beta

Deep links from org Git settings to each project's git repository sync section.

## 1. URL

`/projects/{projectId}/settings#git-repository-sync`

Project settings scrolls to the **Git repository sync** panel when the hash is present.

## 2. UI

Settings → **Git**:

- **Git sync overview** — each project row has a **Git settings** link (hash deep link)
- **Recent sync runs (org-wide)** — each run row has a **Git settings** link to that project

## 3. Tests

Manual / e2e — no new API; project settings section uses `data-testid="git-repository-sync"`.

## 4. Related

- Org overview: [79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md](79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md)
- Org run history: [84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md](84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md)
