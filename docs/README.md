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

**MVP beta milestone (2026-08-07):** Phases 0–5 complete on `main`. **v0.2.18-beta** adds streaming token UX — see [CHANGELOG.md](../CHANGELOG.md).

## Related prototype docs

The repository also contains a **local FastAPI prototype** (shared-context proof):

- [`../SRS.md`](../SRS.md) — prototype SRS  
- [`../ARCHITECTURE.md`](../ARCHITECTURE.md) — prototype architecture  

Those validate the product mechanic; this folder specifies the **Java/Spring + React** SaaS MVP.

Prototype sources now live under `prototype/` (FastAPI + static HTML).
