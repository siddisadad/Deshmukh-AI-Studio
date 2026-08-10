# AI Studio — Design Documentation (MVP SaaS)

Production design pack for **AI Studio for Software Engineering**.  
Read in order; each document builds on the previous.

| # | Document | Description |
|---|---|---|
| 1 | [01-PRD.md](01-PRD.md) | Product requirements for the MVP |
| 2 | [02-SYSTEM-ARCHITECTURE.md](02-SYSTEM-ARCHITECTURE.md) | Modular monolith & Clean Architecture |
| 3 | [03-DATABASE-DESIGN.md](03-DATABASE-DESIGN.md) | ERD, SQL schema, Flyway plan |
| 4 | [04-API-SPECIFICATION.md](04-API-SPECIFICATION.md) | REST API contracts |
| 5 | [05-BACKEND-STRUCTURE.md](05-BACKEND-STRUCTURE.md) | Java/Spring package layout |
| 6 | [06-FRONTEND-STRUCTURE.md](06-FRONTEND-STRUCTURE.md) | React/TypeScript folder & routing |
| 7 | [07-AI-ARCHITECTURE.md](07-AI-ARCHITECTURE.md) | Providers, prompts, context, assistants |
| 8 | [08-UI-WIREFRAMES.md](08-UI-WIREFRAMES.md) | Screen wireframes & UX notes |
| 9 | [09-DEVELOPMENT-ROADMAP.md](09-DEVELOPMENT-ROADMAP.md) | Phased delivery plan |
| 10 | [10-SPRINT-PLANNING.md](10-SPRINT-PLANNING.md) | Sprint-level backlog |
| 11 | [11-CODING-STANDARDS.md](11-CODING-STANDARDS.md) | Engineering standards |
| 12 | [12-TESTING-STRATEGY.md](12-TESTING-STRATEGY.md) | Test pyramid & gates |
| 13 | [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) | Docker/Nginx production deploy |
| 14 | [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) | Staging deploy, Stripe/OIDC dogfood, sign-off |
| 15 | [15-OIDC-IDP-GUIDE.md](15-OIDC-IDP-GUIDE.md) | Okta, Azure AD, Google, Auth0 OIDC setup |
| 16 | [16-SAML-SSO-STUB.md](16-SAML-SSO-STUB.md) | SAML SSO port stub (dev/CI; full SAML backlog) |
| 17 | [17-LOG-ARCHIVE-GUIDE.md](17-LOG-ARCHIVE-GUIDE.md) | Loki export cron + S3 object-store backend |
| 18 | [18-JOB-WORKER-AUTOSCALING-GUIDE.md](18-JOB-WORKER-AUTOSCALING-GUIDE.md) | Background job worker scaling playbook |
| 19 | [19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md) | Scheduled off-site chat thread archive sync |
| 20 | [20-SAML-SP-BINDING-GUIDE.md](20-SAML-SP-BINDING-GUIDE.md) | SAML SP-initiated login (ACS POST + SP metadata) |
| 21 | [21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md](21-OBSERVABILITY-LONG-TERM-ARCHIVE-GUIDE.md) | Glacier lifecycle + cross-region archive replication |
| 22 | [22-BILLING-USAGE-INVOICES-GUIDE.md](22-BILLING-USAGE-INVOICES-GUIDE.md) | AI usage metering history + Stripe invoices |
| 23 | [23-STREAMING-TOKEN-UX-GUIDE.md](23-STREAMING-TOKEN-UX-GUIDE.md) | SSE streaming token UX + usage metadata |
| 24 | [24-STAGING-PROVIDER-PROBES-GUIDE.md](24-STAGING-PROVIDER-PROBES-GUIDE.md) | Automated Stripe/OIDC/SAML/SMTP staging probes |
| 25 | [25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md](25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md) | Chat thread retention policy + legal hold |
| 26 | [26-SAML-SIGNING-GUIDE.md](26-SAML-SIGNING-GUIDE.md) | SAML signed AuthnRequest + encrypted assertions |
| 27 | [27-LOKI-MULTI-REGION-QUERY-GUIDE.md](27-LOKI-MULTI-REGION-QUERY-GUIDE.md) | Real-time multi-region Loki query |
| 28 | [28-BILLING-SEAT-USAGE-METERING-GUIDE.md](28-BILLING-SEAT-USAGE-METERING-GUIDE.md) | Seat metering + usage-based AI overage |
| 29 | [29-PROVIDER-NATIVE-STREAMING-GUIDE.md](29-PROVIDER-NATIVE-STREAMING-GUIDE.md) | OpenAI/Anthropic native SSE streaming |
| 30 | [30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md](30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md) | Compliance gzip export before retention purge |
| 31 | [31-STAGING-LIVE-SIGNOFF-GUIDE.md](31-STAGING-LIVE-SIGNOFF-GUIDE.md) | Automated live-host sign-off + report |
| 32 | [32-STRIPE-METERED-PRICES-SYNC-GUIDE.md](32-STRIPE-METERED-PRICES-SYNC-GUIDE.md) | Stripe metered seat + AI overage sync |
| 33 | [33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md](33-FEDERATED-GRAFANA-LOKI-RULER-GUIDE.md) | Federated Grafana dashboards + Loki ruler fan-out |
| 34 | [34-MULTI-PROVIDER-ROUTING-GUIDE.md](34-MULTI-PROVIDER-ROUTING-GUIDE.md) | Multi-provider AI routing and failover |
| 35 | [35-THREAD-EXPORT-REDACTION-GUIDE.md](35-THREAD-EXPORT-REDACTION-GUIDE.md) | Thread export redaction policies |
| 36 | [36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md](36-AI-PROVIDER-HEALTH-CIRCUIT-GUIDE.md) | AI provider health probes and circuit breaking |
| 37 | [37-BILLING-USAGE-DASHBOARDS-GUIDE.md](37-BILLING-USAGE-DASHBOARDS-GUIDE.md) | Usage-based billing Grafana dashboards |
| 38 | [38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md](38-STAGING-SIGNOFF-S3-ARCHIVE-GUIDE.md) | Staging sign-off report archival in S3 |
| 39 | [39-ALERTMANAGER-ONCALL-GUIDE.md](39-ALERTMANAGER-ONCALL-GUIDE.md) | Alertmanager on-call routing and cross-cluster alerts |
| 40 | [40-ADAPTIVE-AI-ROUTING-GUIDE.md](40-ADAPTIVE-AI-ROUTING-GUIDE.md) | Adaptive latency-based AI provider routing |
| 41 | [41-BILLING-ANOMALY-FORECAST-GUIDE.md](41-BILLING-ANOMALY-FORECAST-GUIDE.md) | Billing anomaly alerts and cost forecasting |
| 42 | [42-SLO-ERROR-BUDGET-GUIDE.md](42-SLO-ERROR-BUDGET-GUIDE.md) | SLO dashboards and error budget alerts |
| 43 | [43-STAGING-SIGNOFF-MATRIX-GUIDE.md](43-STAGING-SIGNOFF-MATRIX-GUIDE.md) | Multi-environment sign-off matrix |
| 44 | [44-THREAD-EXPORT-WATERMARK-DLP-GUIDE.md](44-THREAD-EXPORT-WATERMARK-DLP-GUIDE.md) | Export watermarking and DLP scanning |
| 45 | [45-COST-AWARE-AI-ROUTING-GUIDE.md](45-COST-AWARE-AI-ROUTING-GUIDE.md) | Cost-aware routing and provider quotas |
| 46 | [46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md](46-TOKEN-BUDGET-ORG-ROUTING-GUIDE.md) | Token budget caps and org routing policies |
| 47 | [47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md](47-MODEL-ROUTING-PROMPT-CACHE-GUIDE.md) | Model-specific routing and prompt cache |
| 48 | [48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md](48-PROVIDER-NATIVE-CACHE-CROSS-REGION-GUIDE.md) | Provider-native prompt cache and cross-region routing |
| 49 | [49-ORG-AI-ROUTING-UI-GUIDE.md](49-ORG-AI-ROUTING-UI-GUIDE.md) | Org AI routing policy UI and region overrides |
| 50 | [50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md](50-AI-POLICY-AUDIT-APPROVAL-GUIDE.md) | AI routing policy audit log and change approvals |
| 51 | [51-AI-POLICY-SIMULATION-GUIDE.md](51-AI-POLICY-SIMULATION-GUIDE.md) | AI routing policy simulation / dry-run before apply |
| 52 | [52-AI-POLICY-SIMULATION-AUDIT-GATE-GUIDE.md](52-AI-POLICY-SIMULATION-AUDIT-GATE-GUIDE.md) | Simulation audit trail and rollout gates |
| 53 | [53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md](53-AI-POLICY-CANARY-ROLLOUT-GUIDE.md) | Policy canary rollout / gradual provider shifts |
| 54 | [54-AI-POLICY-CANARY-HOOKS-GUIDE.md](54-AI-POLICY-CANARY-HOOKS-GUIDE.md) | Automated canary promotion / rollback hooks |
| 55 | [55-SLO-MULTI-WINDOW-TENANT-GUIDE.md](55-SLO-MULTI-WINDOW-TENANT-GUIDE.md) | Multi-window burn-rate policies / SLO per tenant |
| 56 | [56-STRIPE-REVENUE-RECONCILIATION-DUNNING-GUIDE.md](56-STRIPE-REVENUE-RECONCILIATION-DUNNING-GUIDE.md) | Stripe revenue reconciliation and dunning automation |
| 57 | [57-SSO-MULTI-IDP-METADATA-GUIDE.md](57-SSO-MULTI-IDP-METADATA-GUIDE.md) | SSO multi-IdP and metadata refresh automation |
| 58 | [58-THREAD-DLP-SIEM-EXPORT-GUIDE.md](58-THREAD-DLP-SIEM-EXPORT-GUIDE.md) | Org DLP connectors and SIEM export |
| 59 | [59-STAGING-SIGNOFF-RELEASE-GATE-GUIDE.md](59-STAGING-SIGNOFF-RELEASE-GATE-GUIDE.md) | Scheduled sign-off cron and release gate |
| 60 | [60-PLUGIN-MARKETPLACE-GUIDE.md](60-PLUGIN-MARKETPLACE-GUIDE.md) | Plugin marketplace and third-party packs |
| 61 | [61-K8S-HPA-WORKER-AUTOSCALING-GUIDE.md](61-K8S-HPA-WORKER-AUTOSCALING-GUIDE.md) | K8s HPA and cloud-native worker autoscaling |
| 62 | [62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md](62-RAG-LARGE-CORPUS-EMBEDDING-GUIDE.md) | RAG large corpus and embedding operations |
| 63 | [63-CODE-METADATA-RAG-GUIDE.md](63-CODE-METADATA-RAG-GUIDE.md) | Code metadata manifest RAG ingestion |
| 64 | [64-GIT-CODE-METADATA-SYNC-GUIDE.md](64-GIT-CODE-METADATA-SYNC-GUIDE.md) | Git sync and webhook code metadata reindex |
| 65 | [65-GITLAB-BITBUCKET-SYNC-GUIDE.md](65-GITLAB-BITBUCKET-SYNC-GUIDE.md) | GitLab and Bitbucket sync connectors |
| 66 | [66-GIT-SYNC-CONTENT-HYDRATION-GUIDE.md](66-GIT-SYNC-CONTENT-HYDRATION-GUIDE.md) | Git sync file snippet hydration |
| 67 | [67-GIT-WEBHOOK-DELTA-SYNC-GUIDE.md](67-GIT-WEBHOOK-DELTA-SYNC-GUIDE.md) | Webhook incremental delta sync |
| 68 | [68-GIT-SCHEDULED-SYNC-GUIDE.md](68-GIT-SCHEDULED-SYNC-GUIDE.md) | Scheduled git sync cron |
| 69 | [69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md](69-GIT-PER-PROJECT-SYNC-INTERVAL-GUIDE.md) | Per-project git sync interval |
| 70 | [70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md](70-GIT-PER-PROJECT-SCHEDULED-SYNC-TOGGLE-GUIDE.md) | Per-project scheduled sync toggle |
| 71 | [71-GIT-PATH-IGNORE-PATTERNS-GUIDE.md](71-GIT-PATH-IGNORE-PATTERNS-GUIDE.md) | Git path ignore patterns |
| 72 | [72-GIT-SYNC-FAILED-SCHEDULED-RETRY-GUIDE.md](72-GIT-SYNC-FAILED-SCHEDULED-RETRY-GUIDE.md) | Git sync failed scheduled retry |
| 73 | [73-GIT-PATH-INCLUDE-PATTERNS-GUIDE.md](73-GIT-PATH-INCLUDE-PATTERNS-GUIDE.md) | Git path include patterns |
| 74 | [74-GIT-SYNC-RUN-HISTORY-GUIDE.md](74-GIT-SYNC-RUN-HISTORY-GUIDE.md) | Git sync run history |
| 75 | [75-ORG-GIT-CREDENTIALS-GUIDE.md](75-ORG-GIT-CREDENTIALS-GUIDE.md) | Organization git host credentials |
| 76 | [76-GIT-STAGING-PROBE-GUIDE.md](76-GIT-STAGING-PROBE-GUIDE.md) | Git staging probes and credential audit |
| 77 | [77-GIT-WEBHOOK-SECRET-UX-GUIDE.md](77-GIT-WEBHOOK-SECRET-UX-GUIDE.md) | Git webhook secret UX and disconnect |
| 78 | [78-GIT-SYNC-RUN-FILTERS-GUIDE.md](78-GIT-SYNC-RUN-FILTERS-GUIDE.md) | Git sync run source/status filters |
| 79 | [79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md](79-ORG-GIT-SYNC-OVERVIEW-GUIDE.md) | Organization git sync overview dashboard |
| 80 | [80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md](80-ORG-GIT-SYNC-OVERVIEW-FILTERS-GUIDE.md) | Org git sync overview filters |
| 81 | [81-GIT-SYNC-RUN-PAGINATION-GUIDE.md](81-GIT-SYNC-RUN-PAGINATION-GUIDE.md) | Git sync run offset pagination |
| 82 | [82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md](82-ORG-GIT-SYNC-RETRY-FAILED-GUIDE.md) | Org bulk retry failed git syncs |
| 83 | [83-ORG-GIT-SYNC-OVERVIEW-EXPORT-GUIDE.md](83-ORG-GIT-SYNC-OVERVIEW-EXPORT-GUIDE.md) | Org git sync overview export |
| 84 | [84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md](84-ORG-GIT-SYNC-RUN-HISTORY-GUIDE.md) | Org-wide git sync run history |
| 85 | [85-ORG-GIT-SYNC-RUN-EXPORT-GUIDE.md](85-ORG-GIT-SYNC-RUN-EXPORT-GUIDE.md) | Org git sync run export |
| 86 | [86-ORG-GIT-SYNC-RUN-PROJECT-FILTER-GUIDE.md](86-ORG-GIT-SYNC-RUN-PROJECT-FILTER-GUIDE.md) | Org sync run project filter UI |
| 87 | [87-ORG-GIT-SYNC-PROJECT-LINKS-GUIDE.md](87-ORG-GIT-SYNC-PROJECT-LINKS-GUIDE.md) | Org git sync project settings deep links |
| 88 | [88-ORG-GIT-SYNC-OVERVIEW-FAILED-ACTIONS-GUIDE.md](88-ORG-GIT-SYNC-OVERVIEW-FAILED-ACTIONS-GUIDE.md) | Org overview failed sync actions |
| 89 | [89-ORG-GIT-SYNC-OVERVIEW-ENABLED-FILTER-GUIDE.md](89-ORG-GIT-SYNC-OVERVIEW-ENABLED-FILTER-GUIDE.md) | Org overview enabled filter + summary chips |
| 90 | [90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md](90-ORG-GIT-SYNC-OVERVIEW-SCHEDULED-SYNC-FILTER-GUIDE.md) | Org overview scheduled sync filter |
| 91 | [91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md](91-ORG-GIT-BULK-ENABLE-SCHEDULED-SYNC-GUIDE.md) | Org bulk enable scheduled sync |
| 92 | [92-ORG-GIT-BULK-DISABLE-SCHEDULED-SYNC-GUIDE.md](92-ORG-GIT-BULK-DISABLE-SCHEDULED-SYNC-GUIDE.md) | Org bulk disable scheduled sync |
| 93 | [93-ORG-GIT-SYNC-PER-PROJECT-SCHEDULED-TOGGLE-GUIDE.md](93-ORG-GIT-SYNC-PER-PROJECT-SCHEDULED-TOGGLE-GUIDE.md) | Org per-project scheduled sync toggle |
| 94 | [94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md](94-ORG-GIT-SYNC-OVERVIEW-INTERVAL-FILTER-GUIDE.md) | Org overview custom interval filter |
| 95 | [95-ORG-GIT-SYNC-CLEAR-CUSTOM-INTERVAL-GUIDE.md](95-ORG-GIT-SYNC-CLEAR-CUSTOM-INTERVAL-GUIDE.md) | Org per-project clear custom interval |
| 96 | [96-ORG-GIT-BULK-SCHEDULED-FILTER-SCOPE-GUIDE.md](96-ORG-GIT-BULK-SCHEDULED-FILTER-SCOPE-GUIDE.md) | Org bulk scheduled sync filter scope |
| 97 | [97-ORG-GIT-SYNC-SET-CUSTOM-INTERVAL-GUIDE.md](97-ORG-GIT-SYNC-SET-CUSTOM-INTERVAL-GUIDE.md) | Org per-project set custom interval |
| 98 | [98-ORG-GIT-RETRY-FAILED-FILTER-SCOPE-GUIDE.md](98-ORG-GIT-RETRY-FAILED-FILTER-SCOPE-GUIDE.md) | Org retry failed filter scope |
| 99 | [99-ORG-GIT-BULK-CLEAR-INTERVAL-FILTER-SCOPE-GUIDE.md](99-ORG-GIT-BULK-CLEAR-INTERVAL-FILTER-SCOPE-GUIDE.md) | Org bulk clear interval filter scope |
| 100 | [100-ORG-GIT-BULK-SET-INTERVAL-FILTER-SCOPE-GUIDE.md](100-ORG-GIT-BULK-SET-INTERVAL-FILTER-SCOPE-GUIDE.md) | Org bulk set interval filter scope |
| 101 | [101-ORG-GIT-BULK-ACTIONS-SUMMARY-GUIDE.md](101-ORG-GIT-BULK-ACTIONS-SUMMARY-GUIDE.md) | Org bulk actions summary preview |
| 102 | [102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md](102-ORG-GIT-SYNC-OVERVIEW-FILTER-PRESETS-GUIDE.md) | Org overview filter preset chips |
| 103 | [103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md](103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md) | Org overview filter URL sharing |

**MVP beta milestone (2026-08-07):** Phases 0–5 complete on `main`. **v0.2.97-beta** adds overview filter preset chips — see [CHANGELOG.md](../CHANGELOG.md).

## Related prototype docs

The repository also contains a **local FastAPI prototype** (shared-context proof):

- [`../SRS.md`](../SRS.md) — prototype SRS  
- [`../ARCHITECTURE.md`](../ARCHITECTURE.md) — prototype architecture  

Those validate the product mechanic; this folder specifies the **Java/Spring + React** SaaS MVP.

Prototype sources now live under `prototype/` (FastAPI + static HTML).
