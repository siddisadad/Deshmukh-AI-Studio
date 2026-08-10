# Development Roadmap
## AI Studio for Software Engineering — MVP → Growth

Realistic roadmap for a small startup (2–4 engineers). Ordered by risk reduction and vertical slices.

---

## 1. Guiding Rules

1. Ship vertical slices (auth → project → requirement AI) over horizontal layers alone.
2. Keep the modular monolith until AI load or team size forces extraction.
3. Prefer Mock AI in CI; real providers in staging.
4. Every phase ends with demos against acceptance criteria in `01-PRD.md`.

---

## 2. Phase Overview

| Phase | Name | Outcome |
|---|---|---|
| **0** | Foundations | Repo, CI, Docker Compose, Flyway baseline, OpenAPI stub |
| **1** | Identity & tenancy | Register/login/JWT/RBAC/org/project shell |
| **2** | Work management | Requirements, tasks/Kanban, documents CRUD |
| **3** | AI workspace | Providers, context, assistants, chat, BA actions |
| **4** | Hardening | Rate limits, audit, tests, deploy guide validation |
| **5** | Beta | Staging, feedback, polish UX |
| **6+** | Growth | RAG, billing, SSO, plugins |

---

## 3. Phase 0 — Foundations

**Deliverables**
- Monorepo or dual folders: `backend/`, `frontend/`, `docs/`, `docker-compose.yml`
- Spring Boot skeleton + React Vite skeleton
- PostgreSQL + Flyway `V1`
- GitHub Actions: compile, unit tests, frontend lint/build
- Health endpoint + README run instructions

**Exit criteria:** `docker compose up` boots API + DB; empty SPA loads.

---

## 4. Phase 1 — Identity & Tenancy

**Deliverables**
- Register, login, refresh, logout, forgot/reset password
- Profile + theme preference
- Organizations + memberships
- Projects CRUD + archive + dashboard counts (zeros OK)
- Frontend auth flow + protected routes + app shell

**Exit criteria:** Two users cannot access each other’s projects (authz tests green).

---

## 5. Phase 2 — Work Management

**Deliverables**
- Requirements CRUD + editor UI
- Tasks + labels + Kanban
- Documents CRUD + markdown editor
- Context assets upsert API (UI minimal OK)

**Exit criteria:** Full non-AI SDLC path works: requirement → tasks → doc.

---

## 6. Phase 3 — AI Workspace

**Deliverables**
- `AiProviderPort` + Mock + one real provider
- Prompt templates + ContextBuilder + ConversationManager
- Four assistants listed and chatable
- BA improve / stories / AC
- Docs generate + Developer/QA shortcut endpoints (or chat-only if timebox)

**Exit criteria:** Same context influences BA and Developer answers; history persists.

---

## 7. Phase 4 — Hardening

**Deliverables**
- Rate limiting, CORS lockdown, audit logs
- Global error model, request IDs
- Unit + API integration tests for critical paths
- Nginx + compose production profile
- OpenAPI published artifact

**Exit criteria:** Security smoke checklist passed; deploy guide followed successfully on clean VM/local.

---

## 8. Phase 5 — Private Beta

**Deliverables**
- Staging environment + monitoring basics
- Onboarding empty states polished
- Bug bash + prompt tuning
- Backup script for Postgres
- Compose dry-runs (prod + staging local build) in CI
- GHCR staging deploy script; images published on `main`
- Growth E2E (multi-thread chat, RAG, billing, plugins, mock SSO)
- Cloud Agent environment build + validation

**Exit criteria:** Friendly users complete first-run journey without assistance.

**Status (2026-08-07):** Complete on `main` — see [CHANGELOG.md](../CHANGELOG.md) `v0.1.0-beta`.

---

## 9. Phase 6+ — Growth Backlog

Shipped on `main` (v0.1.0-beta baseline); remaining work is production hardening and depth.

| Item | Status on `main` | Next depth |
|---|---|---|
| RAG over documents/code metadata | pgvector index + search UI + large corpus tuning ([62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md](62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md)) | — |
| Streaming AI responses | SSE chat + reconnect/retry UX + token metering UI ([23-STREAMING-TOKEN-UX-GUIDE.md](23-STREAMING-TOKEN-UX-GUIDE.md)), provider-native OpenAI/Anthropic streams ([29-PROVIDER-NATIVE-STREAMING-GUIDE.md](29-PROVIDER-NATIVE-STREAMING-GUIDE.md)), multi-provider routing / failover ([34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md)), provider health probes / circuit breaking ([36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md](36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md)), adaptive latency-based routing ([40-ADAPTIVE-AI-ROUTING-GUIDE.md](40-ADAPTIVE-AI-ROUTING-GUIDE.md)), cost-aware routing / provider quotas ([45-COST-AWARE-AI-ROUTING-GUIDE.md](45-COST-AWARE-AI-ROUTING-GUIDE.md)), token budget caps / org routing ([46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md)), model-specific routing / prompt cache ([47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md](47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md)), provider-native prompt cache / cross-region routing ([48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md](48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md)), org routing policy UI / region overrides ([49-ORG-AI-ROUTING-UI-GUIDE.md](49-ORG-AI-ROUTING-UI-GUIDE.md)), routing policy audit log / change approvals ([50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md](50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md)), policy simulation / dry-run before apply ([51-AI-POLICY-SIMULATION-GUIDE.md](51-AI-POLICY-SIMULATION-GUIDE.md)), simulation audit trail / rollout gates ([52-AI-POLICY-SIMULATION-AUDIT-GATE-GUIDE.md](52-AI-POLICY-SIMULATION-AUDIT-GATE-GUIDE.md)), policy canary rollout / gradual provider shifts ([53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md](53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md)), automated canary promotion / rollback hooks ([54-AI-POLICY-CANARY-HOOKS-GUIDE.md](54-AI-POLICY-CANARY-HOOKS-GUIDE.md)) | — |
| Observability | JSON logs, Prometheus, Grafana, Loki alerts, retention, queue metrics, export archives, Glacier lifecycle + cross-region ([21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md](21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md)), multi-region Loki query ([27-LOKI-MULTI-REGION-QUERY-GUIDE.md](27-LOKI-MULTI-REGION-QUERY-GUIDE.md)), federated Grafana + Loki ruler fan-out ([33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md](33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md)), Alertmanager on-call routing ([39-ALERTMANAGER-ONCALL-GUIDE.md](39-ALERTMANAGER-ONCALL-GUIDE.md)), SLO dashboards + error budget alerts ([42-SLO-ERROR-BUDGET-GUIDE.md](42-SLO-ERROR-BUDGET-GUIDE.md)), multi-window burn-rate / per-tenant SLO ([55-SLO-MULTI-WINDOW-TENANT-GUIDE.md](55-SLO-MULTI-WINDOW-TENANT-GUIDE.md)) | — |
| Billing / plans | Stripe checkout + webhook + usage history + invoice list ([22-BILLING-USAGE-INVOICES-GUIDE.md](22-BILLING-USAGE-INVOICES-GUIDE.md)), seat metering + AI overage ([28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md)), Stripe metered prices sync ([32-STRIPE-METERED-PRICES-SYNC-GUIDE.md](32-STRIPE-METERED-PRICES-SYNC-GUIDE.md)), usage-based billing dashboards ([37-BILLING-USAGE-DASHBOARDS-GUIDE.md](37-BILLING-USAGE-DASHBOARDS-GUIDE.md)), billing anomaly alerts + cost forecasting ([41-BILLING-ANOMALY-FORECAST-GUIDE.md](41-BILLING-ANOMALY-FORECAST-GUIDE.md)), Stripe revenue reconciliation / dunning automation ([56-STRIPE-REVENUE-RECONCILIATION-DUNNING-GUIDE.md](56-STRIPE-REVENUE-RECONCILIATION-DUNNING-GUIDE.md)) | — |
| SSO (OIDC/SAML) | OIDC adapter + IdP guides + SAML stub + SP binding + signed AuthnRequest / encrypted assertions ([26-SAML-SIGNING-GUIDE.md](26-SAML-SIGNING-GUIDE.md)), multi-IdP / metadata refresh automation ([57-SSO-MULTI-IDP-METADATA-GUIDE.md](57-SSO-MULTI-IDP-METADATA-GUIDE.md)) | — |
| Multi-thread conversations | Per-assistant threads + search + share + export + archive sync ([19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md)), retention + legal hold ([25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md](25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md)), compliance export on purge ([30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md](30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md)), thread export redaction policies ([35-THREAD-EXPORT-REDACTION-GUIDE.md](35-THREAD-EXPORT-REDACTION-GUIDE.md)), export watermarking / DLP ([44-THREAD-EXPORT-WATERMARK-DLP-GUIDE.md](44-THREAD-EXPORT-WATERMARK-DLP-GUIDE.md)), richer DLP connectors / SIEM export ([58-THREAD-DLP-SIEM-EXPORT-GUIDE.md](58-THREAD-DLP-SIEM-EXPORT-GUIDE.md)) | — |
| Plugin architecture | SPI + org toggles, marketplace / third-party packs ([60-PLUGIN-MARKETPLACE-GUIDE.md](60-PLUGIN-MARKETPLACE-GUIDE.md)) | — |
| Background AI jobs | Reindex + doc generate queue + dedicated worker + SKIP LOCKED multi-replica + queue metrics + autoscaling playbook ([18-JOB-WORKER-AUTOSCALING-GUIDE.md](18-JOB-WORKER-AUTOSCALING-GUIDE.md)), K8s HPA / cloud-native autoscaling ([61-K8S-HPA-WORKER-AUTOSCALING-GUIDE.md](61-K8S-HPA-WORKER-AUTOSCALING-GUIDE.md)) | — |
| Staging dogfood | Automated gates + manual checklist ([14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md)), provider probes ([24-STAGING-PROVIDER-PROBES-GUIDE.md](24-STAGING-PROVIDER-PROBES-GUIDE.md)), live-host sign-off automation ([31-STAGING-LIVE-SIGNOFF-GUIDE.md](31-STAGING-LIVE-SIGNOFF-GUIDE.md)), sign-off report S3 archival ([38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md](38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md)), multi-environment sign-off matrix ([43-STAGING-SIGNOFF-MATRIX-GUIDE.md](43-STAGING-SIGNOFF-MATRIX-GUIDE.md)), scheduled sign-off cron / release gate ([59-STAGING-SIGNOFF-RELEASE-GATE-GUIDE.md](59-STAGING-SIGNOFF-RELEASE-GATE-GUIDE.md)) | — |

---

## 10. Phase 7 — Production depth (complete)

| Item | Status on `main` | Next depth |
|---|---|---|
| Code metadata RAG | Manifest API + `CODE_FILE` indexing ([63-CODE-METADATA-RAG-GUIDE.md](63-CODE-METADATA-RAG-GUIDE.md)), Git sync + webhook reindex ([64-GIT-CODE-METADATA-SYNC-GUIDE.md](64-GIT-CODE-METADATA-SYNC-GUIDE.md)), GitLab/Bitbucket connectors ([65-GITLAB-BITBUCKET-SYNC-GUIDE.md](65-GITLAB-BITBUCKET-SYNC-GUIDE.md)), Git content hydration ([66-GIT-SYNC-CONTENT-HYDRATION-GUIDE.md](66-GIT-SYNC-CONTENT-HYDRATION-GUIDE.md)), webhook delta sync ([67-GIT-WEBHOOK-DELTA-SYNC-GUIDE.md](67-GIT-WEBHOOK-DELTA-SYNC-GUIDE.md)), scheduled sync cron ([68-GIT-SCHEDULED-SYNC-GUIDE.md](68-GIT-SCHEDULED-SYNC-GUIDE.md)), per-project sync interval ([69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md](69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md)) | — |

---

## 11. Phase 8 — Git sync depth (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Scheduled sync controls | Per-project interval ([69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md](69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md)), per-project scheduled toggle ([70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md](70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md)) | — |
| Path filtering | Git ignore patterns ([71-GIT-PATH-IGNORE-PATTERNS-GUIDE.md](71-GIT-PATH-IGNORE-PATTERNS-GUIDE.md)) | — |
| Failed sync retry | Scheduled retry on failure ([72-GIT-SYNC-FAILED-SCHEDULED-RETRY-GUIDE.md](72-GIT-SYNC-FAILED-SCHEDULED-RETRY-GUIDE.md)) | — |

---

## 12. Phase 9 — Git sync scope (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Path scope | Include patterns ([73-GIT-PATH-INCLUDE-PATTERNS-GUIDE.md](73-GIT-PATH-INCLUDE-PATTERNS-GUIDE.md)) | — |
| Sync audit | Run history API + UI ([74-GIT-SYNC-RUN-HISTORY-GUIDE.md](74-GIT-SYNC-RUN-HISTORY-GUIDE.md)) | — |

---

## 13. Phase 10 — Git tenancy (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Org git credentials | Per-org PAT + API base URL ([75-ORG-GIT-CREDENTIALS-GUIDE.md](75-ORG-GIT-CREDENTIALS-GUIDE.md)) | — |
| Connection probe | Org + project git link test + staging probes ([76-GIT-STAGING-PROBE-GUIDE.md](76-GIT-STAGING-PROBE-GUIDE.md)) | — |

---

## 14. Phase 11 — Git link operations (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Webhook UX | Regenerate, copy, disconnect ([77-GIT-WEBHOOK-SECRET-UX-GUIDE.md](77-GIT-WEBHOOK-SECRET-UX-GUIDE.md)) | — |

---

## 15. Phase 12 — Git sync visibility (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Run filters | Source/status filters + refresh UI ([78-GIT-SYNC-RUN-FILTERS-GUIDE.md](78-GIT-SYNC-RUN-FILTERS-GUIDE.md)) | — |
| Run pagination | Offset/limit page response + load more UI ([81-GIT-SYNC-RUN-PAGINATION-GUIDE.md](81-GIT-SYNC-RUN-PAGINATION-GUIDE.md)) | — |

---

## 16. Phase 13 — Org git sync dashboard (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Sync overview | Org-wide project git link + last sync dashboard ([79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md](79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md)) | — |
| Overview filters | Linked/provider/last-sync filters + refresh UI ([80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md)) | — |

---

## 17. Phase 14 — Org git sync operations (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Retry failed | Bulk enqueue sync for failed last-sync links ([82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md](82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md)) | — |

---

## 18. Phase 15 — Org git sync reporting (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Overview export | CSV/JSON download with overview filters ([83-ORG-GIT-SYNC-OVERVIEW-EXPORT-GUIDE.md](83-ORG-GIT-SYNC-OVERVIEW-EXPORT-GUIDE.md)) | — |

---

## 19. Phase 16 — Org git sync run history (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Org-wide runs | Cross-project sync run list with filters + pagination ([84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md](84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md)) | — |

---

## 20. Phase 17 — Org git sync run reporting (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Run export | CSV/JSON download with run filters ([85-ORG-GIT-SYNC-RUN-EXPORT-GUIDE.md](85-ORG-GIT-SYNC-RUN-EXPORT-GUIDE.md)) | — |

---

## 21. Phase 18 — Org git sync run filters (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Project filter UI | Project dropdown on org sync runs + export ([86-ORG-GIT-SYNC-RUN-PROJECT-FILTER-GUIDE.md](86-ORG-GIT-SYNC-RUN-PROJECT-FILTER-GUIDE.md)) | — |

---

## 22. Phase 19 — Org git sync navigation (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Project git deep links | Overview + run rows link to project git settings ([87-ORG-GIT-SYNC-PROJECT-LINKS-GUIDE.md](87-ORG-GIT-SYNC-PROJECT-LINKS-GUIDE.md)) | — |

---

## 23. Phase 20 — Org overview failed actions (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Failed row actions | Per-project retry + view failed runs + clickable failed count ([88-ORG-GIT-SYNC-OVERVIEW-FAILED-ACTIONS-GUIDE.md](88-ORG-GIT-SYNC-OVERVIEW-FAILED-ACTIONS-GUIDE.md)) | — |

---

## 24. Phase 21 — Org overview enabled filter (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Enabled filter + chips | `enabled` query param, dropdown, linked/enabled summary quick filters ([89-ORG-GIT-SYNC-OVERVIEW-ENABLED-FILTER-GUIDE.md](89-ORG-GIT-SYNC-OVERVIEW-ENABLED-FILTER-GUIDE.md)) | — |

---

## 25. Phase 22 — Org overview scheduled sync filter (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Scheduled sync filter | `scheduledSyncEnabled` param, dropdown, summary count + chip ([90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md](90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md)) | — |

---

## 26. Phase 23 — Org bulk enable scheduled sync (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Bulk enable scheduled | `enable-scheduled-sync` API, `manualSyncLinks` count, overview button ([91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md](91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md)) | — |

---

## 27. Phase 24 — Org bulk disable scheduled sync (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Bulk disable scheduled | `disable-scheduled-sync` API, overview button ([92-ORG-GIT-BULK-DISABLE-SCHEDULED-SYNC-GUIDE.md](92-ORG-GIT-BULK-DISABLE-SCHEDULED-SYNC-GUIDE.md)) | — |

---

## 28. Phase 25 — Org per-project scheduled toggle (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Row scheduled actions | Per-project enable/disable scheduled sync APIs + overview buttons ([93-ORG-GIT-SYNC-PER-PROJECT-SCHEDULED-TOGGLE-GUIDE.md](93-ORG-GIT-SYNC-PER-PROJECT-SCHEDULED-TOGGLE-GUIDE.md)) | — |

---

## 29. Phase 26 — Org overview custom interval filter (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Interval filter | `customSyncInterval` param, dropdown, summary count + chip ([94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md](94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md)) | — |

---

## 30. Phase 27 — Org clear custom interval (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Clear interval action | Per-project clear interval API + overview row button ([95-ORG-GIT-SYNC-CLEAR-CUSTOM-INTERVAL-GUIDE.md](95-ORG-GIT-SYNC-CLEAR-CUSTOM-INTERVAL-GUIDE.md)) | — |

---

## 31. Phase 28 — Org bulk scheduled filter scope (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Filter-scoped bulk scheduled | Overview filter params on bulk enable/disable APIs + UI ([96-ORG-GIT-BULK-SCHEDULED-FILTER-SCOPE-GUIDE.md](96-ORG-GIT-BULK-SCHEDULED-FILTER-SCOPE-GUIDE.md)) | — |

---

## 32. Phase 29 — Org set custom interval (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Set interval action | Per-project set interval API + overview dialog ([97-ORG-GIT-SYNC-SET-CUSTOM-INTERVAL-GUIDE.md](97-ORG-GIT-SYNC-SET-CUSTOM-INTERVAL-GUIDE.md)) | — |

---

## 33. Phase 30 — Org retry failed filter scope (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Filter-scoped retry failed | Overview filter params on retry-failed API + UI ([98-ORG-GIT-RETRY-FAILED-FILTER-SCOPE-GUIDE.md](98-ORG-GIT-RETRY-FAILED-FILTER-SCOPE-GUIDE.md)) | — |

---

## 34. Phase 31 — Org bulk clear interval filter scope (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Filter-scoped bulk clear interval | Overview filter params on clear-interval API + UI ([99-ORG-GIT-BULK-CLEAR-INTERVAL-FILTER-SCOPE-GUIDE.md](99-ORG-GIT-BULK-CLEAR-INTERVAL-FILTER-SCOPE-GUIDE.md)) | — |

---

## 35. Phase 32 — Org bulk set interval filter scope (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Filter-scoped bulk set interval | Overview filter params on set-interval API + dialog ([100-ORG-GIT-BULK-SET-INTERVAL-FILTER-SCOPE-GUIDE.md](100-ORG-GIT-BULK-SET-INTERVAL-FILTER-SCOPE-GUIDE.md)) | — |

---

## 36. Phase 33 — Org bulk actions summary (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Bulk actions preview | Filter-scoped targeted counts API + export + overview UI ([101-ORG-GIT-BULK-ACTIONS-SUMMARY-GUIDE.md](101-ORG-GIT-BULK-ACTIONS-SUMMARY-GUIDE.md)) | — |

---

## 37. Phase 34 — Org overview filter presets (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Filter preset chips | Combined overview filter quick-apply chips ([102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md](102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md)) | — |

---

## 38. Phase 35 — Org overview filter URL (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Shareable filter URL | Overview filter query params + copy link ([103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md](103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md)) | — |

---

## 39. Phase 36 — Org overview active filter chips (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Active filter chips | Removable per-dimension filter chips ([104-ORG-GIT-SYNC-OVERVIEW-ACTIVE-FILTER-CHIPS-GUIDE.md](104-ORG-GIT-SYNC-OVERVIEW-ACTIVE-FILTER-CHIPS-GUIDE.md)) | — |

---

## 40. Phase 37 — Org sync run filter URL (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Shareable run filter URL | Run history query params + copy link ([105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md](105-ORG-GIT-SYNC-RUN-FILTER-URL-GUIDE.md)) | — |

---

## 41. Phase 38 — Org sync run active filter chips (started)

| Item | Status on `main` | Next depth |
|---|---|---|
| Active run filter chips | Removable per-dimension run filter chips ([106-ORG-GIT-SYNC-RUN-ACTIVE-FILTER-CHIPS-GUIDE.md](106-ORG-GIT-SYNC-RUN-ACTIVE-FILTER-CHIPS-GUIDE.md)) | — |

---

## 13. Dependency Graph (simplified)

```
Foundations → Identity → Projects → Requirements/Tasks/Docs → AI → Hardening → Beta
```

AI can prototype in parallel behind feature flag after Projects exist, but do not merge without authz.

---

## 14. Staffing Suggestion

| Role | Focus |
|---|---|
| Backend lead | Security, modules, AI ports |
| Frontend lead | Shell, Kanban, chat UX |
| Full-stack | Requirements/docs slices |
| Part-time DevOps | Compose, CI, staging |

---

## 15. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | MVP-centered roadmap |

**Previous:** `08-UI-WIREFRAMES.md` · **Next:** `10-SPRINT-PLANNING.md`
