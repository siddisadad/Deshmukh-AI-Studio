# E2E smoke (Playwright)

Critical private-beta journeys from `docs/12-TESTING-STRATEGY.md`.

## Prerequisites

- API on `:8080` with `AI_PROVIDER=mock` (default)
- UI on `:5173` (Vite) **or** compose frontend on `:8088`

## Local (dev servers)

```bash
# terminal 1
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# terminal 2
cd frontend && npm run dev -- --host 0.0.0.0

# terminal 3
cd e2e
npm ci
npx playwright install chromium
npm test
```

## Compose stack

```bash
docker compose up -d --build
cd e2e && npm ci && npx playwright install chromium
E2E_BASE_URL=http://localhost:8088 npm test
```

## CI

The `e2e` job in `.github/workflows/ci.yml` builds the compose stack (mock AI) and runs Chromium smoke against `http://localhost:8088`.
