# Organization Git Sync Filter URL Navigation Guide

**Version:** v0.2.112-beta

Browser back and forward navigation for org git sync filter URLs on Settings → **Git**.

## 1. Behavior

| Navigation | Expected result |
|---|---|
| Back to URL with overview params | Overview filter chips and data match query string |
| Back to URL without overview params | Active overview filters clear |
| Back to URL with run params + hash | Run filter chips apply; runs section scrolls into view |
| Back to URL without run params | Active run filters clear |
| Forward after back | Filters re-apply from the forward URL |

Filter URL hydration re-runs on `popstate` (browser back/forward). In-app filter changes still use `replace` navigation so the history stack stays usable.

## 2. E2E

`e2e/tests/org-git-sync-filter-url.spec.ts` — scenario **browser back and forward restore filter chips from URL**

## 3. Related

- Filter URL E2E: [115-ORG-GIT-SYNC-FILTER-URL-E2E-GUIDE.md](115-ORG-GIT-SYNC-FILTER-URL-E2E-GUIDE.md)
- Shared URL helpers: `frontend/src/features/settings/gitSyncFilterUrl.ts`
- Testing strategy: [12-TESTING-STRATEGY.md](12-TESTING-STRATEGY.md)
