# Changelog

All notable changes to the **Production MVP** (Spring Boot + React) track.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning targets [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
