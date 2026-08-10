# Organization Git Sync Filter URL E2E Guide

**Version:** v0.2.110-beta

Playwright coverage for shareable org git sync filter URLs on Settings → **Git**.

## 1. Spec

`e2e/tests/org-git-sync-filter-url.spec.ts`

| Scenario | What it verifies |
|---|---|
| Overview query params | `linked` + `lastSync` apply active filter chips on load |
| Run query params + hash | `runSource` + `runStatus` apply; runs section visible |
| Combined page filters | Overview + run params show page toolbar and both chip rows |
| Copy overview link | Clipboard URL round-trips `provider` + `enabled` filters |
| Copy page link | Clipboard URL includes overview + run params and `#org-git-sync-runs` |
| Clear all filters | Page toolbar disappears after **Clear all filters** |

## 2. Run locally

```bash
# API + frontend (mock AI) via compose, or Vite on :5173
export E2E_BASE_URL=http://localhost:8088  # or http://localhost:5173
cd e2e && npm ci && npm run install:browsers && npm test -- tests/org-git-sync-filter-url.spec.ts
```

CI runs all `e2e/tests/*.spec.ts` on every push/PR.

## 3. Related

- Full page filter state: [108-ORG-GIT-SYNC-FULL-PAGE-FILTER-STATE-GUIDE.md](108-ORG-GIT-SYNC-FULL-PAGE-FILTER-STATE-GUIDE.md)
- Overview URL: [103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md](103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md)
- Run URL: [105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md](105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md)
- Testing strategy: [12-TESTING-STRATEGY.md](12-TESTING-STRATEGY.md)
