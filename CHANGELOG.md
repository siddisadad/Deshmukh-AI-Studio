# Changelog

All notable changes to the **Production MVP** (Spring Boot + React) track.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning targets [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.13-beta] — 2026-08-08

### Added

- `docs/18-JOB-WORKER-AUTOSCALING-GUIDE.md` — worker scaling playbook (queue depth, alerts, `WORKER_REPLICAS`)
- `scripts/worker-scale-hint.sh` — suggest replica count from `aistudio.jobs.queue.depth`

## [0.2.12-beta] — 2026-08-08

### Added

- Loki S3 object-store config (`monitoring/loki-config-s3.yml`) + `docker-compose.monitoring-s3.yml` overlay
- `scripts/export-loki-logs.sh` — gzipped NDJSON export with optional S3 upload (`LOKI_ARCHIVE_S3_URI`)
- `docs/17-LOG-ARCHIVE-GUIDE.md` — cron archive and S3 backend playbook

## [0.2.11-beta] — 2026-08-08

### Added

- Prometheus gauges `aistudio.jobs.queue.depth` by job status (pending/running/failed, etc.)
- Grafana dashboard panel + Alertmanager rule for high pending queue depth

## [0.2.10-beta] — 2026-08-08

### Added

- Bulk project thread archive export (`GET /projects/{id}/conversations/export`) — JSON or Markdown
- Chat UI: "Export all" for current assistant's visible threads

## [0.2.9-beta] — 2026-08-08

### Added

- SAML SSO port stub (`SSO_PROVIDER=saml`, `SAML_STUB_MODE=true`) — dev/CI adapter until full SP binding
- `docs/16-SAML-SSO-STUB.md` — stub env vars and smoke test; points production to OIDC

## [0.2.8-beta] — 2026-08-08

### Added

- Conversation thread export (`GET /conversations/{id}/export`) — JSON or Markdown download
- Chat UI: export button on each thread in the sidebar
- `docs/15-OIDC-IDP-GUIDE.md` — step-by-step OIDC setup for Okta, Entra ID, Google, Auth0

## [0.2.7-beta] — 2026-08-08

### Added

- Private conversation threads (`visibility: PRIVATE`) — visible only to creator within a project
- Chat UI: "Private thread" checkbox and lock icon on private threads

## [0.2.6-beta] — 2026-08-08

### Added

- Loki compactor log retention (`LOKI_RETENTION_PERIOD`, default 720h / 30 days)
- `max_query_lookback` aligned with retention for Grafana Explore

## [0.2.5-beta] — 2026-08-08

### Added

- Horizontal job worker scaling: Postgres `FOR UPDATE SKIP LOCKED` claiming, `locked_by` / `locked_at`
- Stale worker lock reclaim (`JOB_STALE_LOCK_SECONDS`, `JOB_MAX_ATTEMPTS`)
- `docker compose up --scale worker=N` and `WORKER_REPLICAS` for staging GHCR deploy
- `AISTUDIO_JOBS_WORKER_ID` for stable worker identity in multi-replica setups

## [0.2.4-beta] — 2026-08-08

### Added

- Read-only conversation share links (`POST/DELETE /conversations/{id}/share`, public `GET /shared/conversations/{token}`)
- Public SPA route `/shared/chat/:token` for read-only shared threads
- `CONVERSATION_SHARE_TTL_SECONDS` (default 7 days) for share link expiry

### Fixed

- E2E: share button `data-testid` no longer matches `chat-thread-*` list locator

## [0.2.3-beta] — 2026-08-08

### Added

- `scripts/validate-workspace.sh` — fast lint/build/unit-test gate (no Docker)
- CI: frontend `npm run lint` + workspace validation in `environment-config` job
- Loki ruler log-based alerts (`ApiErrorLogsHigh`, `ApiWarnLogsHigh`, `ApiLogsMissing`) → Alertmanager

### Changed

- Cloud Agent `install.sh` attempts Docker Compose install when missing; clearer message when daemon is unavailable

### Fixed

- CI `environment-config` uses Java 21; phased staging dry-run startup fixes flaky API health checks
- Staging dogfood sign-off example `IMAGE_TAG` updated to `v0.2.3-beta`
- Deployment guide `IMAGE_TAG` examples aligned with current releases

## [0.2.2-beta] — 2026-08-08

### Added

- Dedicated background job worker container (`docker-compose.worker.yml`) — API disables in-process polling in `prod` profile; worker runs `prod,worker` profiles
- `AISTUDIO_JOBS_WORKER_ENABLED` env gate for `BackgroundJobWorker` scheduling
- Deploy/staging scripts verify worker health alongside API smoke checks

## [0.2.1-beta] — 2026-08-08

### Added

- `docs/14-STAGING-DOGFOOD-GUIDE.md` — staging deploy, automated gates, Stripe/OIDC dogfood, sign-off checklist
- Conversation thread search: `GET /projects/{id}/conversations?q=` matches title and message content
- Chat UI thread search field with debounced filter
- Alertmanager service in `docker-compose.monitoring.yml` wired to Prometheus alert rules
- Loki + Promtail log shipping for API container JSON stdout; Grafana Loki datasource

### Fixed

- Chat thread search `TextField` uses MUI `slotProps` (fixes frontend build on MUI v6)
- E2E smoke: thread search test id no longer matches `chat-thread-*` list locator

## [0.2.0-beta] — 2026-08-08

### Added

- Chat SSE reconnect: token refresh retry, stream recovery polling, cancel in-flight stream
- Backend continues AI generation when SSE client disconnects mid-stream (assistant message still persisted)
- Optional `docker-compose.monitoring.yml` with Prometheus + Grafana
- Grafana dashboard, alert rules, and `monitoring/README.md`
- `METRICS_SCRAPE_TOKEN` for internal `/actuator/prometheus` scrapes (JWT alternative)
- `scripts/staging-dogfood.sh` automated staging validation gates
- `scripts/write-prometheus-token.sh` for Prometheus bearer token file
- SMTP mail adapter (`MAIL_PROVIDER=smtp`) for password-reset emails
- `scripts/api-smoke.sh` automated register → project API journey (used in staging dogfood)

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
