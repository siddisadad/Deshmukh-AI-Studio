# Changelog

All notable changes to the **Production MVP** (Spring Boot + React) track.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning targets [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.30-beta] — 2026-08-08

### Added

- Thread export redaction policies — `none`, `pii`, `secrets`, `standard` via `CHAT_EXPORT_REDACTION_POLICY` or `redaction` query param
- Separate `CHAT_EXPORT_COMPLIANCE_REDACTION_POLICY` for retention purge archives (default `none`)
- `ThreadExportRedactor` — regex redaction for emails, phones, tokens, API keys in exported messages
- `scripts/scheduled-chat-archive.sh` forwards `CHAT_EXPORT_REDACTION_POLICY` to bulk export API
- `docs/35-THREAD-EXPORT-REDACTION-GUIDE.md`

## [0.2.29-beta] — 2026-08-08

### Added

- Multi-provider AI routing — `AI_PROVIDER=routing` + `AI_PROVIDER_CHAIN` or per-provider `AI_PROVIDER_FALLBACKS`
- `AiProviderRegistry`, `RoutingAiProvider`, `AiProviderConfiguration` — ordered failover for `generate()` and `stream()`
- `docs/34-MULTI-PROVIDER-ROUTING-GUIDE.md`

## [0.2.28-beta] — 2026-08-08

### Added

- Federated Grafana dashboard (`aistudio-federated`) — per-region API up, 5xx, ERROR/WARN log rates
- `scripts/write-grafana-prometheus-regions.sh` — regional Prometheus datasources
- `scripts/write-grafana-federated-dashboard.sh` — generates federated dashboard JSON
- `scripts/sync-loki-ruler-regions.sh` — fan-out `loki-alerts.yml` to regional Loki rulers
- `docs/33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md`

## [0.2.27-beta] — 2026-08-08

### Added

- Stripe metered prices — checkout attaches seat + AI overage metered line items when configured
- `POST /api/v1/billing/stripe/sync-metered-usage` with `BILLING_USAGE_SYNC_TOKEN`
- Subscription item ID tracking + Stripe UsageRecord reporting for seats and AI overage
- `scripts/sync-stripe-metered-prices.sh`, `scripts/scheduled-stripe-usage-sync.sh`
- Flyway `V19` plan metered price columns + org subscription item IDs
- `docs/32-STRIPE-METERED-PRICES-SYNC-GUIDE.md`

## [0.2.26-beta] — 2026-08-08

### Added

- `scripts/staging-signoff.sh` — full live-host sign-off (dogfood gates + HTTPS, security headers, billing/SSO probes, SSE stream smoke)
- JSON/Markdown sign-off reports (`STAGING_SIGNOFF_REPORT_DIR`)
- `staging-dogfood.sh` step 7: optional `STAGING_SIGNOFF=1` full automation; integrated into `staging-dry-run.sh`
- `docs/31-STAGING-LIVE-SIGNOFF-GUIDE.md`

## [0.2.25-beta] — 2026-08-08

### Added

- Retention purge compliance export — `{"complianceExport":true}` returns gzip JSON archive before delete
- Compliance metadata on exported threads (`purgeReason`, `retentionExpiresAt`, `purgedAt`)
- `scheduled-chat-retention.sh` supports `CHAT_RETENTION_COMPLIANCE_EXPORT` + optional S3 upload
- `docs/30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md`

## [0.2.24-beta] — 2026-08-08

### Added

- OpenAI native SSE streaming with `stream_options.include_usage` — token counts on stream completion
- Anthropic native SSE streaming — usage from `message_start` / `message_delta` events
- Provider stream unit tests (embedded HTTP server fixtures)
- `docs/29-PROVIDER-NATIVE-STREAMING-GUIDE.md`

## [0.2.23-beta] — 2026-08-08

### Added

- Plan `max_seats` with invite enforcement and billing overview member counts
- Per-seat monthly price estimates (`price_cents_per_seat_monthly`)
- AI action overage metering (`price_cents_per_ai_action_overage`, `ai_usage_daily.overage_count`)
- PRO/TEAM soft daily limits with overage tracking and period cost estimates
- `docs/28-BILLING-SEAT-USAGE-METERING-GUIDE.md`

## [0.2.22-beta] — 2026-08-08

### Added

- `scripts/query-loki-multi-region.sh` — merge LogQL `query_range` results across regional Loki endpoints
- `scripts/write-grafana-loki-regions.sh` — provision extra Grafana Loki datasources from `LOKI_QUERY_REGIONS`
- Staging provider probes check each regional Loki `/ready` when configured
- `docs/27-LOKI-MULTI-REGION-QUERY-GUIDE.md`

## [0.2.21-beta] — 2026-08-08

### Added

- SAML signed HTTP-Redirect AuthnRequest (`Signature` / `SigAlg` query params) when `SAML_SP_PRIVATE_KEY` + `SAML_SP_CERTIFICATE` set
- Optional encrypted assertion decryption via `SAML_WANT_ENCRYPTED_ASSERTIONS` + IdP encryption cert in metadata
- `SamlAuthnRedirectBuilder`, `SamlPemUtils`; IdP metadata loader parses encryption `KeyDescriptor`
- Staging env validation for SP key/cert when signing or encryption enabled
- `docs/26-SAML-SIGNING-GUIDE.md`

## [0.2.20-beta] — 2026-08-08

### Added

- Project `chatRetentionDays` policy with per-thread `retentionExpiresAt` computation
- Conversation `legalHold` — blocks delete and automated purge
- `POST /projects/{id}/conversations/retention-purge` + `scripts/scheduled-chat-retention.sh`
- Chat UI legal-hold toggle; project settings retention + manual purge
- `docs/25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md`

## [0.2.19-beta] — 2026-08-08

### Added

- `scripts/staging-provider-probes.sh` — automated Stripe/OIDC/SAML/SMTP readiness probes
- Integrated into `staging-dogfood.sh` (step 5/7) and `staging-dry-run.sh`
- `docs/24-STAGING-PROVIDER-PROBES-GUIDE.md` — probe matrix and troubleshooting

## [0.2.18-beta] — 2026-08-08

### Added

- SSE `done` event **usage** metadata: `inputTokens`, `outputTokens`, `streamChars`, `deltaCount`
- Chat UI: live char count during stream, RAF delta batching, post-stream token hints in footer
- Mock AI provider token estimates for metering UX
- `docs/23-STREAMING-TOKEN-UX-GUIDE.md` — SSE usage fields and smoke test

## [0.2.17-beta] — 2026-08-08

### Added

- `GET /organizations/{id}/billing/usage` — daily AI action metering history (up to 90 days)
- `GET /organizations/{id}/billing/invoices` — Stripe invoice list for org owners
- Billing settings UI: 30-day usage table, invoice list, Stripe checkout redirect, portal button
- `docs/22-BILLING-USAGE-INVOICES-GUIDE.md` — metering and invoice playbook

## [0.2.16-beta] — 2026-08-08

### Added

- `scripts/apply-s3-archive-lifecycle.sh` — S3 Glacier / Deep Archive lifecycle for export prefixes
- `scripts/enable-s3-cross-region-replication.sh` — prefix-scoped CRR to DR bucket
- `monitoring/s3-lifecycle-archive-example.json` + `s3-replication-role-policy-example.json`
- `docs/21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md` — tiering, restore, and DR playbook

## [0.2.15-beta] — 2026-08-08

### Added

- Full SAML SP-initiated binding (`SAML_STUB_MODE=false`): HTTP-Redirect AuthnRequest, HTTP-POST ACS (`/auth/sso/saml/acs`), SP metadata endpoint
- `docs/20-SAML-SP-BINDING-GUIDE.md` — IdP wiring, env vars, and flow diagram
- `java-saml` integration for assertion validation and metadata-driven IdP settings

## [0.2.14-beta] — 2026-08-08

### Added

- `scripts/scheduled-chat-archive.sh` — cron-friendly bulk thread export per project with optional S3 upload (`CHAT_ARCHIVE_S3_URI`)
- `docs/19-CHAT-ARCHIVE-SYNC-GUIDE.md` — service account, cron, and off-site retention playbook

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
