# Changelog

All notable changes to the **Production MVP** (Spring Boot + React) track.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning targets [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Optional `docker-compose.monitoring.yml` with Prometheus + Grafana
- Grafana dashboard, alert rules, and `monitoring/README.md`
- `METRICS_SCRAPE_TOKEN` for internal `/actuator/prometheus` scrapes (JWT alternative)
- `scripts/staging-dogfood.sh` automated staging validation gates
- `scripts/write-prometheus-token.sh` for Prometheus bearer token file

## [0.1.2-beta] — 2026-08-08

### Added

- Prometheus metrics at `/actuator/prometheus` (JWT required; not exposed on nginx edge)
- `scripts/post-deploy-smoke.sh` for post-deploy health + info checks

### Fixed

- `validate-staging-env.sh` rejects HTTP/localhost `CORS_ORIGINS` (matches prod `ProductionCorsValidator`)

## [0.1.1-beta] — 2026-08-08

### Added

- Stripe billing adapter (`BILLING_PROVIDER=stripe`) with checkout, customer portal, and webhook handler
- OIDC SSO adapter (`SSO_PROVIDER=oidc`) with discovery, token exchange, and userinfo
- Flyway `V13`: optional `plans.stripe_price_id` column
- `scripts/validate-staging-env.sh` for pre-deploy env checks (Stripe/OIDC aware)
- Compose staging/prod overlays pass Stripe and OIDC environment variables to API

### Changed

- Phase 6+ roadmap documents shipped vs next-depth backlog items
- `staging-ghcr-deploy.sh` runs env validation after loading `.env`

### Fixed

- GHCR publish includes git tag ref (`v0.1.0-beta`) alongside semver (`0.1.0-beta`)
- `staging-ghcr-deploy.sh` loads `.env` before required-variable checks
- Stripe billing adapter compile fix; stable billing integration tests

## [0.1.0-beta] — 2026-08-07

### Added

- Full MVP SPA: auth, projects, requirements, Kanban tasks, documents, AI chat (4 assistants)
- Streaming SSE chat, multi-thread conversations, RAG (pgvector), background AI jobs
- Billing (FREE/PRO/TEAM), mock SSO, plugin SPI with org enablement
- Prod + staging Docker Compose overlays, backup/restore scripts
- CI: backend ITs, frontend Vitest, Playwright smoke (growth journeys + mock SSO)
- `deploy-dry-run.sh`, `staging-dry-run.sh`, `staging-ghcr-deploy.sh`
- Cloud Agent environment: `.cursor/environment.json`, install/start scripts, validation in CI
- GHCR image publish on `main` and `v*` tags

### Changed

- Phase 5 beta polish: onboarding checklist, dashboard batch aggregates, accessibility, prompt v2
- Mock AI routes by `metadata.action` before keyword heuristics

### Fixed

- MUI 7 Switch test IDs for plugins settings (TypeScript + Docker frontend build)
- SSO/E2E compose env (`BILLING_APP_BASE_URL`, `SSO_APP_BASE_URL` on `:8088`)
- GHCR publish includes git tag ref (`v0.1.0-beta`) alongside semver (`0.1.0-beta`)
- `staging-ghcr-deploy.sh` loads `.env` before required-variable checks

## [0.0.1] — 2026-08-06

- Initial design pack and repository foundations (Phase 0–4)
