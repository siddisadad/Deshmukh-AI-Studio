# Changelog

All notable changes to the **Production MVP** (Spring Boot + React) track.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Versioning targets [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.82-beta] — 2026-08-09

### Added

- Org git sync project settings deep links — overview and sync run rows link to `#git-repository-sync`
- Project settings scrolls to git repository sync section when hash is present
- `docs/87-ORG-GIT-SYNC-PROJECT-LINKS-GUIDE.md`, Phase 19 roadmap

## [0.2.81-beta] — 2026-08-09

### Added

- Org git sync run project filter UI — project dropdown on org-wide sync runs (list, load more, export)
- `docs/86-ORG-GIT-SYNC-RUN-PROJECT-FILTER-GUIDE.md`, Phase 18 roadmap

## [0.2.80-beta] — 2026-08-09

### Added

- Org git sync run export — `GET .../git-sync-runs/export` with `format=csv|json` and run filters (up to 1000 rows)
- Git settings — **Export CSV** and **Export JSON** on org-wide sync runs section
- `docs/85-ORG-GIT-SYNC-RUN-EXPORT-GUIDE.md`, Phase 17 roadmap

## [0.2.79-beta] — 2026-08-09

### Added

- Org-wide git sync run history — `GET .../git-sync-runs` with pagination, source/status filters, optional `projectId`
- Git settings — **Recent sync runs (org-wide)** with filters, refresh, and load more
- `docs/84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md`, Phase 16 roadmap

## [0.2.78-beta] — 2026-08-09

### Added

- Org git sync overview export — `GET .../git-sync-overview/export` with `format=csv|json` and overview filters
- Git settings — **Export CSV** and **Export JSON** on overview section (uses current filters)
- `docs/83-ORG-GIT-SYNC-OVERVIEW-EXPORT-GUIDE.md`, Phase 15 roadmap

## [0.2.77-beta] — 2026-08-09

### Added

- Org bulk retry failed git syncs — `POST .../git-sync-overview/retry-failed` enqueues background sync jobs
- Git settings — **Retry failed syncs** for OWNER/ADMIN when overview shows failures
- `docs/82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md`, Phase 14 roadmap

## [0.2.76-beta] — 2026-08-09

### Added

- Git sync run pagination — `offset` on `GET .../git-link/sync-runs` with page response (`totalCount`, `hasMore`)
- Project settings — “Showing N of total” and Load more for sync runs
- `docs/81-GIT-SYNC-RUN-PAGINATION-GUIDE.md`

## [0.2.75-beta] — 2026-08-09

### Added

- Org git sync overview filters — `linked`, `provider`, `lastSyncStatus` on `GET .../git-sync-overview`
- Git settings — linked/provider/last-sync filter dropdowns on overview section
- `docs/80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md`

## [0.2.74-beta] — 2026-08-09

### Added

- Org git sync overview — `GET /organizations/{orgId}/git-sync-overview` with per-project link and last-sync status
- Git settings — org-wide sync overview section with refresh and project settings links
- `docs/79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md`, Phase 13 roadmap

## [0.2.73-beta] — 2026-08-09

### Added

- Git sync run filters — `source` and `status` query params on `GET .../git-link/sync-runs`
- Project settings — source/status filters, refresh button, failed-run highlighting
- `docs/78-GIT-SYNC-RUN-FILTERS-GUIDE.md`

## [0.2.72-beta] — 2026-08-09

### Added

- Git webhook secret UX — `POST .../git-link/regenerate-webhook-secret` and `DELETE .../git-link`
- Project settings — copy URL/secret, reveal secret, regenerate, disconnect
- `docs/77-GIT-WEBHOOK-SECRET-UX-GUIDE.md`

## [0.2.71-beta] — 2026-08-09

### Added

- Git credential rotation audit — `organization_git_credential_events` (CREATED, TOKEN_ROTATED, UPDATED, DELETED)
- `GET /api/v1/organizations/{orgId}/git-credentials/events`
- Settings → Git rotation audit list
- Staging git probes in `staging-provider-probes.sh` (platform token + API credential test)
- `docs/76-GIT-STAGING-PROBE-GUIDE.md`

## [0.2.70-beta] — 2026-08-09

### Added

- Organization git credentials — per-org PATs with optional API base URL (self-managed GitLab)
- Credential resolution: org credential → platform env token
- Connection test — `POST .../git-credentials/{provider}/test` and `POST .../git-link/test`
- Settings UI — Git credentials page (`/settings/git`)
- Project settings — Test connection button
- `docs/75-ORG-GIT-CREDENTIALS-GUIDE.md` — Phase 10 git tenancy opener

## [0.2.69-beta] — 2026-08-09

### Added

- Git sync run history — `project_git_sync_runs` audit table
- Records manual, scheduled, and webhook sync attempts with status and file count
- `GET /api/v1/projects/{id}/git-link/sync-runs`
- Project settings UI — recent sync runs list
- `docs/74-GIT-SYNC-RUN-HISTORY-GUIDE.md`

## [0.2.68-beta] — 2026-08-09

### Added

- Git path include patterns — per-project Ant-style globs to scope sync
- Empty include list syncs all paths; include applied before ignore filters
- `PUT git-link` — `pathIncludePatterns` and `clearPathIncludePatterns`
- Project settings UI — path include patterns field
- `docs/73-GIT-PATH-INCLUDE-PATTERNS-GUIDE.md`

## [0.2.67-beta] — 2026-08-09

### Added

- Git sync failed scheduled retry — failed links bypass per-project interval on scheduler tick
- Config — `GIT_SYNC_FAILED_SCHEDULED_RETRY` (default `true`)
- `docs/72-GIT-SYNC-FAILED-SCHEDULED-RETRY-GUIDE.md`

## [0.2.66-beta] — 2026-08-09

### Added

- Git path ignore patterns — per-project Ant-style globs on git links
- Full, webhook delta, and background sync skip matching paths
- `PUT git-link` — `pathIgnorePatterns` and `clearPathIgnorePatterns`
- Project settings UI — path ignore patterns (one per line)
- `docs/71-GIT-PATH-IGNORE-PATTERNS-GUIDE.md`

## [0.2.65-beta] — 2026-08-09

### Added

- Per-project scheduled git sync toggle — `scheduled_sync_enabled` on git links
- Scheduler skips links with scheduled sync disabled; webhooks and manual sync unchanged
- `PUT git-link` — optional `scheduledSyncEnabled`
- Project settings UI — scheduled sync enable/disable
- `docs/70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md`

## [0.2.64-beta] — 2026-08-09

### Added

- Per-project git sync interval — optional `scheduled_sync_interval_minutes` on git links
- Scheduled enqueue skips links until per-project or platform interval elapsed since `last_synced_at`
- `PUT git-link` — `scheduledSyncIntervalMinutes` (15–10080) and `clearScheduledSyncInterval`
- Project settings UI — scheduled sync interval field
- `docs/69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md`

## [0.2.63-beta] — 2026-08-09

### Added

- Git scheduled sync cron — enqueue `CODE_METADATA_SYNC` for enabled git links on interval
- Config — `GIT_SYNC_SCHEDULED_ENABLED`, `GIT_SYNC_SCHEDULED_INTERVAL_MS`
- Skips projects with pending `CODE_METADATA_SYNC` jobs
- `docs/68-GIT-SCHEDULED-SYNC-GUIDE.md`

## [0.2.62-beta] — 2026-08-09

### Added

- Git webhook incremental delta sync — parse push payloads for changed/removed paths
- `GIT_SYNC_WEBHOOK_DELTA` config (default `true`)
- `GitMetadataPort.fetchFilesByPaths` — per-path fetch on GitHub/GitLab/Bitbucket/mock
- `ProjectCodeMetadataService.applyDeltaInternal` — merge upserts and path deletes
- `docs/67-GIT-WEBHOOK-DELTA-SYNC-GUIDE.md`

## [0.2.61-beta] — 2026-08-09

### Added

- Git sync file content hydration — fetch snippets from GitHub/GitLab/Bitbucket after tree listing
- Config — `GIT_SYNC_FETCH_CONTENT`, `GIT_SYNC_MAX_SNIPPET_BYTES`, `GIT_SYNC_MAX_CONTENT_FETCH_BYTES`
- `GitMetadataPort.hydrateFileContents` — per-provider raw file fetch with truncation
- `docs/66-GIT-SYNC-CONTENT-HYDRATION-GUIDE.md`

## [0.2.60-beta] — 2026-08-09

### Added

- GitLab and Bitbucket code metadata sync connectors
- `GitMetadataRegistry` — routes sync by project `provider`
- `GitlabGitMetadataProvider` and `BitbucketGitMetadataProvider` (REST tree fetch)
- V37 migration — `gitlab` / `bitbucket` provider values on `project_git_links`
- `POST /api/v1/git/webhook/gitlab/{projectId}` — `X-Gitlab-Token` verification
- `POST /api/v1/git/webhook/bitbucket/{projectId}` — HMAC signature verification
- `PUT git-link` optional `provider` field (`github`, `gitlab`, `bitbucket`)
- Project settings UI — Git provider selector
- `docs/65-GITLAB-BITBUCKET-SYNC-GUIDE.md`

## [0.2.59-beta] — 2026-08-09

### Added

- Git code metadata sync — V35 `project_git_links`, mock/GitHub tree fetch
- `GET/PUT /api/v1/projects/{id}/git-link` — repository link + webhook secret
- `POST /api/v1/projects/{id}/git-link/sync` and `/sync/async`
- `POST /api/v1/git/webhook/github/{projectId}` — push webhook → `CODE_METADATA_SYNC` job
- Background job type `CODE_METADATA_SYNC`
- Project settings Git repository sync UI
- `docs/64-GIT-CODE-METADATA-SYNC-GUIDE.md`

## [0.2.58-beta] — 2026-08-09

### Added

- Code metadata RAG — V34 `project_code_files` table and `CODE_FILE` knowledge source type
- `GET/PUT /api/v1/projects/{id}/code-metadata` — manifest replace with automatic RAG reindex
- `RAG_MAX_CODE_FILES_PER_PROJECT` (default 500)
- Project settings UI — code metadata manifest JSON upload
- `docs/63-CODE-METADATA-RAG-GUIDE.md`

## [0.2.57-beta] — 2026-08-09

### Added

- RAG large corpus — V33 HNSW index on `knowledge_chunks.embedding`
- Configurable chunking — `RAG_CHUNK_SIZE`, `RAG_CHUNK_OVERLAP`, `RAG_MAX_CHUNKS_PER_PROJECT`
- Batched embeddings — `EMBEDDING_BATCH_SIZE` for OpenAI and mock providers
- Corpus limits — `maxChunksPerProject` and `corpusLimitReached` on knowledge status/reindex APIs
- Higher search limit — `RAG_SEARCH_MAX_K` (default 32) for semantic search
- Prometheus metrics — `aistudio.knowledge.embeddings.texts` and `aistudio.knowledge.embeddings.batches`
- Project settings UI — indexed/max chunk display and corpus limit warning
- `docs/62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md`

## [0.2.56-beta] — 2026-08-09

### Added

- Job queue autoscale API — `GET /api/v1/ops/jobs/queue` with pending/running/failed + `suggestedReplicas`
- K8s manifests — `deploy/kubernetes/worker-deployment.yaml`, Prometheus HPA, KEDA ScaledObject
- Prometheus recording rule `monitoring/job-queue-autoscale-rules.yml`
- Cron script `scheduled-worker-autoscale.sh` with optional `WORKER_AUTOSCALE_APPLY`
- `docs/61-K8S-HPA-WORKER-AUTOSCALING-GUIDE.md`

## [0.2.55-beta] — 2026-08-09

### Added

- Plugin marketplace — V32 migration, `plugin_packs`, `plugin_pack_members`, `organization_plugin_packs`
- JSON pack manifests in `backend/src/main/resources/plugin-packs/`
- Third-party tool packs — Developer Tools (markdown preview, word count) and Compliance Helpers (redaction scan, export checklist)
- `GET /api/v1/plugins/marketplace` and org install/uninstall endpoints
- Pack-gated plugin visibility — tools hidden until pack installed
- Settings UI marketplace section on `/settings/plugins`
- `docs/60-PLUGIN-MARKETPLACE-GUIDE.md`

## [0.2.54-beta] — 2026-08-09

### Added

- Staging sign-off run store — V31 migration, `staging_signoff_runs` audit table
- `POST /api/v1/ops/staging-signoff/submit` — submit sign-off JSON for release gate
- `GET /api/v1/ops/release-gate?imageTag=` — evaluate passing sign-off within max age
- `GET /api/v1/ops/staging-signoff/runs` — recent sign-off history
- Cron script `scheduled-staging-signoff.sh` — single-host or matrix + optional API submit
- Release gate script `release-gate-check.sh` — pre-tag validation
- `latest-signoff.json` / `latest-signoff-matrix.json` pointers from sign-off scripts
- `docs/59-STAGING-SIGNOFF-RELEASE-GATE-GUIDE.md`

## [0.2.53-beta] — 2026-08-09

### Added

- Org DLP connectors — V30 migration, `organization_dlp_connectors` (WEBHOOK / SIEM)
- `thread_export_dlp_events` audit log with `siem_exported_at` tracking
- Richer built-in DLP patterns — credit card, GitHub PAT, Google API key, Slack token
- Org custom regex patterns via `custom_patterns_json` on connectors
- `ThreadExportDlpPolicyService` — records events, org webhook notify, org-level block
- `GET/POST/DELETE /organizations/{id}/dlp/connectors` and `GET /dlp/events`
- `POST /api/v1/exports/siem/run` operator endpoint (`BILLING_USAGE_SYNC_TOKEN`)
- `SiemExportService` + `SiemExportScheduler` batch export to SIEM connectors
- Settings UI `/settings/dlp` for connector and event management
- Cron script `scheduled-siem-export.sh`
- `docs/58-THREAD-DLP-SIEM-EXPORT-GUIDE.md`

## [0.2.52-beta] — 2026-08-09

### Added

- Stripe revenue reconciliation — V28 migration, `billing_reconciliation_runs` audit table
- Internal MTD vs Stripe paid invoice comparison per org (`BillingReconciliationService`)
- `POST /api/v1/billing/stripe/reconcile` operator endpoint
- Dunning automation — `dunning_stage`, `billing_dunning_events` audit log
- Stripe webhook handlers for `invoice.payment_failed` / `invoice.payment_succeeded`
- Staged dunning emails to org owners via `BillingDunningService`
- `POST /api/v1/billing/stripe/dunning/run` and `BillingDunningScheduler`
- Billing settings UI — past-due banner and reconciliation delta display
- Prometheus `aistudio_billing_reconciliation_delta_cents` per-org gauge
- `BillingReconciliationMismatch` alert in `monitoring/billing-alerts.yml`
- Cron scripts `scheduled-stripe-reconciliation.sh`, `scheduled-billing-dunning.sh`
- `docs/56-STRIPE-REVENUE-RECONCILIATION-DUNNING-GUIDE.md`

## [0.2.50-beta] — 2026-08-09

### Added

- Per-organization SLO targets on `organizations` (V27) — availability, latency, threshold seconds
- `GET/PUT /organizations/{id}/slo` and `/settings/slo` UI for owners
- HTTP metrics tagged with `organization_id` via `OrgSloRequestFilter` + observation filter
- Prometheus gauges for per-tenant SLO targets (`aistudio_slo_org_*`)
- Multi-window burn-rate recording rules (5m / 1h / 6h) for platform and tenant
- Multi-window burn-rate alerts — page (14.4×) and ticket (6×) policies
- Grafana **AI Studio SLO (tenant)** dashboard (`uid: aistudio-slo-tenant`)
- `docs/55-SLO-MULTI-WINDOW-TENANT-GUIDE.md`

## [0.2.49-beta] — 2026-08-09

### Added

- Automated canary promotion / rollback hooks — hook settings on subscriptions (V26)
- Per-org canary outcome counters (`org_ai_canary_outcomes`) recorded from chat requests
- `OrgAiCanaryHookService` evaluates thresholds → auto promote/abort + optional webhook
- `PUT/POST /organizations/{id}/ai-policy/canary/hooks` and `canary/evaluate`
- Scheduled hook evaluation — `AI_CANARY_HOOK_EVAL_ENABLED` + interval env vars
- Settings UI — canary automation hooks on `/settings/ai-routing`
- `docs/54-AI-POLICY-CANARY-HOOKS-GUIDE.md`

## [0.2.48-beta] — 2026-08-09

### Added

- AI policy canary rollout — `ai_canary_provider_chain` + `ai_canary_percent` on subscriptions
- Per-conversation sticky routing to canary chain via `OrgAiRoutingPolicyService`
- `PUT/POST/DELETE /organizations/{id}/ai-policy/canary` promote and abort endpoints
- Settings UI — canary rollout section on `/settings/ai-routing`
- `docs/53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md`

## [0.2.47-beta] — 2026-08-09

### Added

- AI policy simulation audit trail — `org_ai_policy_simulations` + `GET /organizations/{id}/ai-policy/simulations`
- Staged rollout gates — `AI_POLICY_SIMULATION_GATE_ENABLED` requires passing `simulationId` on `PUT`
- Simulate response adds `simulationId` and `gatePassed`; policy response adds `simulationGateEnabled`
- Settings UI — simulation history, gate note, save blocked until preview passes
- `docs/52-AI-POLICY-SIMULATION-AUDIT-GATE-GUIDE.md`

## [0.2.46-beta] — 2026-08-09

### Added

- AI routing policy simulation — `POST /organizations/{id}/ai-policy/simulate` dry-runs proposed changes
- `OrgAiPolicyRoutingPreview` resolves effective provider chains and flags missing providers
- Settings UI — **Preview changes** panel on `/settings/ai-routing` with chain diff and approval hint
- `docs/51-AI-POLICY-SIMULATION-GUIDE.md`

## [0.2.45-beta] — 2026-08-09

### Added

- AI routing policy audit log — `org_ai_policy_changes` table + `GET /organizations/{id}/ai-policy/changes`
- Change approvals — `AI_POLICY_CHANGE_APPROVAL_ENABLED` queues ADMIN proposals for OWNER approve/reject
- `POST /organizations/{id}/ai-policy/pending/approve` and `pending/reject`
- Settings UI — pending banner, approve/reject, change history on `/settings/ai-routing`
- `docs/50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md`

## [0.2.44-beta] — 2026-08-09

### Added

- Org deploy region override — `ai_deploy_region` on subscriptions + `deployRegion` on ai-policy API
- `OrgAiRoutingContext` applies org region to cross-region provider chain resolution
- AI routing policy UI — `/settings/ai-routing` for provider chain, token budget, model map, deploy region
- `docs/49-ORG-AI-ROUTING-UI-GUIDE.md`

## [0.2.43-beta] — 2026-08-09

### Added

- Provider-native prompt caching — `AI_PROVIDER_NATIVE_PROMPT_CACHE_ENABLED` sends OpenAI/Anthropic `cache_control` on system prompts
- Cross-region AI routing — `AI_CROSS_REGION_ROUTING_ENABLED` with `AISTUDIO_DEPLOY_REGION`, `AI_PROVIDER_ENDPOINT_MAP`, `AI_PROVIDER_REGION_CHAINS`
- Regional provider aliases — `openai-eu`, `anthropic-eu` register from endpoint map with shared API keys
- `AiProviderCrossRegionRegistry`, regional `OpenAiProvider` / `AnthropicProvider` constructors
- `docs/48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md`

## [0.2.42-beta] — 2026-08-09

### Added

- Model-specific routing — `AI_ASSISTANT_MODEL_MAP` maps assistant roles to `provider:model`
- Org model map override — `ai_model_map` on subscriptions + `modelMap` on ai-policy API
- `RoutingAiProvider` prefers mapped provider; OpenAI/Anthropic honor metadata `model`
- Prompt cache — `AI_PROMPT_CACHE_ENABLED` TTL cache for project context and system prompt assembly
- `AiModelRoutingRegistry`, `AiModelRoutingService`, `AiPromptCache`
- `docs/47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md`

## [0.2.41-beta] — 2026-08-09

### Added

- Per-plan daily AI token budgets — `plans.max_ai_tokens_per_day` with usage in `ai_usage_daily.token_count`
- Org token budget override — `organization_subscriptions.daily_token_budget`
- Org-level AI routing — `ai_provider_chain` per org; `GET/PUT /organizations/{id}/ai-policy`
- `OrgAiRoutingContext` + `RoutingAiProvider` org chain resolution per chat request
- Billing overview fields — `aiTokensUsedToday`, `effectiveDailyTokenBudget`, `aiProviderChain`
- `docs/46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md`

## [0.2.40-beta] — 2026-08-09

### Added

- Cost-aware AI routing — `AI_COST_AWARE_ROUTING_ENABLED` prefers lower cost-tier providers in routing chains
- Per-provider daily quotas — `AI_PROVIDER_QUOTAS` skips exhausted providers (UTC day reset)
- Configurable cost tiers — `AI_PROVIDER_COST_TIERS` (defaults: mock=1, openai=5, anthropic=8)
- `GET /assistants/provider-health` — `costTier`, `dailyQuota`, `quotaUsedToday`, `quotaRemaining`, `quotaExhausted`
- `AiProviderCostTierRegistry`, `AiProviderQuotaTracker`
- `docs/45-COST-AWARE-AI-ROUTING-GUIDE.md`

## [0.2.39-beta] — 2026-08-09

### Added

- Thread export watermarking — `CHAT_EXPORT_WATERMARK_ENABLED` embeds `exportId`, `exportedByUserId`, and notice in JSON/Markdown
- Export DLP scanning — `CHAT_EXPORT_DLP_ENABLED` detects SSN, PEM private keys, internal hostnames, AWS secret patterns
- Optional DLP webhook (`CHAT_EXPORT_DLP_WEBHOOK_URL`) and block-on-match (`CHAT_EXPORT_DLP_BLOCK_ON_MATCH`)
- `ThreadExportWatermark`, `ThreadExportDlpScanner`, `ThreadExportDlpNotifier`
- `docs/44-THREAD-EXPORT-WATERMARK-DLP-GUIDE.md`

## [0.2.38-beta] — 2026-08-09

### Added

- Multi-environment sign-off matrix — `staging-signoff-matrix.sh` + `STAGING_SIGNOFF_ENVIRONMENTS`
- Combined matrix JSON/Markdown reports with per-environment sign-off embeds
- `STAGING_SIGNOFF_LABEL` on single-host reports for matrix identification
- `docs/43-STAGING-SIGNOFF-MATRIX-GUIDE.md`

## [0.2.37-beta] — 2026-08-09

### Added

- SLO recording rules — availability (99.5%) and latency (95% <2s) SLIs with 30d windows
- Error budget alerts — budget low/critical and high burn-rate in `monitoring/slo-alerts.yml`
- Grafana dashboard **AI Studio SLO** (`uid: aistudio-slo`)
- `scripts/write-grafana-slo-dashboard.sh`
- `docs/42-SLO-ERROR-BUDGET-GUIDE.md`

## [0.2.36-beta] — 2026-08-08

### Added

- Billing anomaly alerts — `monitoring/billing-alerts.yml` for overage rate, MTD, forecast, and 7-day spike detection
- Cost forecasting metrics — linear month-end forecast for overage actions and estimated cents (plan-weighted)
- Grafana billing dashboard forecast panels
- `docs/41-BILLING-ANOMALY-FORECAST-GUIDE.md`

## [0.2.35-beta] — 2026-08-08

### Added

- Adaptive AI routing — `AI_ADAPTIVE_ROUTING_ENABLED` reorders provider chains by rolling average latency
- `AiProviderLatencyTracker` — per-provider latency samples for routing and health reporting
- `GET /assistants/provider-health` — `averageLatencyMs` and `latencySampleCount` fields
- `docs/40-ADAPTIVE-AI-ROUTING-GUIDE.md`

## [0.2.34-beta] — 2026-08-08

### Added

- Alertmanager on-call integrations — `write-alertmanager-config.sh` generates Slack, PagerDuty, and webhook receivers
- Cross-cluster routing by `severity` and `cluster` label; `PROMETHEUS_CLUSTER_NAME` external label on metric alerts
- `sync-alertmanager-regions.sh` — verify and reload regional Alertmanagers (`ALERTMANAGER_QUERY_REGIONS`)
- `docs/39-ALERTMANAGER-ONCALL-GUIDE.md`

## [0.2.33-beta] — 2026-08-08

### Added

- Staging sign-off report archival — `STAGING_SIGNOFF_S3_URI` uploads JSON + Markdown to S3 after each run
- `docs/38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md`

## [0.2.32-beta] — 2026-08-08

### Added

- Usage-based billing Prometheus metrics — AI action counters, daily/MTD overage gauges, active seats
- Grafana dashboard **AI Studio Billing Usage** (`uid: aistudio-billing`)
- `scripts/write-grafana-billing-dashboard.sh` — validate dashboard JSON
- `docs/37-BILLING-USAGE-DASHBOARDS-GUIDE.md`

## [0.2.31-beta] — 2026-08-08

### Added

- AI provider circuit breaker — skip failing providers in routing chains (`AI_CIRCUIT_BREAKER_*`)
- `GET /api/v1/assistants/provider-health` with optional `probe=true` live connectivity checks
- OpenAI `/v1/models` and Anthropic minimal-message health probes
- `staging-provider-probes.sh` AI provider probe when `AI_PROVIDER` is not `mock`
- `docs/36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md`

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
