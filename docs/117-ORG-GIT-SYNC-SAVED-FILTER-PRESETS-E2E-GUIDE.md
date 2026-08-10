# Organization Git Sync Saved Filter Presets E2E Guide

**Version:** v0.2.113-beta

Playwright coverage for server-synced saved filter presets on Settings → **Git**.

## 1. Spec

`e2e/tests/org-git-sync-saved-filter-presets.spec.ts`

| Scenario | What it verifies |
|---|---|
| Overview preset lifecycle | Save overview filters → apply saved chip → delete |
| Run preset lifecycle | Save run filters → apply saved chip → delete |
| Org-shared preset | OWNER saves with **Share with organization** → chip shows `· Org` |

## 2. Run locally

```bash
export E2E_BASE_URL=http://localhost:8088  # or http://localhost:5173
cd e2e && npm ci && npm run install:browsers && npm test -- tests/org-git-sync-saved-filter-presets.spec.ts
```

CI runs all `e2e/tests/*.spec.ts` on every push/PR.

## 3. Related

- Server-synced presets API: [112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md](112-ORG-GIT-SYNC-FILTER-PRESETS-API-GUIDE.md)
- Org-shared presets: [114-ORG-GIT-SHARED-FILTER-PRESETS-GUIDE.md](114-ORG-GIT-SHARED-FILTER-PRESETS-GUIDE.md)
- Filter URL E2E: [115-ORG-GIT-SYNC-FILTER-URL-E2E-GUIDE.md](115-ORG-GIT-SYNC-FILTER-URL-E2E-GUIDE.md)
- Testing strategy: [12-TESTING-STRATEGY.md](12-TESTING-STRATEGY.md)
