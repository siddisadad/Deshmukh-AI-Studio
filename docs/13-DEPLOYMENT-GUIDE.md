# Deployment Guide
## AI Studio for Software Engineering — MVP

Production-shaped deployment using **Docker Compose + Nginx** on a single VM. Suitable for early SaaS / private beta.

---

## 1. Architecture (Deployed)

```
Internet
   │
   ▼
Nginx (TLS, SPA static, /api proxy)
   │
   ├─► frontend static files
   └─► api:8080 (Spring Boot)
            │
            ├─► postgres:5432
            └─► redis:6379 (optional)
```

External: LLM provider APIs (OpenAI/Anthropic).

---

## 2. Prerequisites

- Docker Engine + Docker Compose v2
- Domain name + DNS A/AAAA record
- TLS certificates (Let’s Encrypt recommended)
- Secrets: `JWT_SECRET`, `DB_PASSWORD`, provider API key

Minimum VM (beta): 2 vCPU, 4 GB RAM, 40 GB SSD.

---

## 3. Repository Layout (deploy artifacts)

```
/
├── docker-compose.yml
├── docker-compose.prod.yml
├── docker-compose.staging.yml
├── .env.example
├── nginx/
│   ├── nginx.conf
│   └── conf.d/aistudio.conf
├── scripts/
│   ├── backup-db.sh
│   ├── restore-db.sh
│   ├── healthcheck.sh
│   └── deploy-dry-run.sh
├── backend/Dockerfile
└── frontend/Dockerfile
```

---

## 4. Environment Variables

Copy `.env.example` → `.env` (never commit `.env`).

```bash
# Database
DB_NAME=aistudio
DB_USER=aistudio
DB_PASSWORD=change-me-strong
POSTGRES_PORT=5432

# API
JWT_SECRET=use-a-long-random-string-at-least-32-chars
CORS_ORIGINS=https://app.example.com
AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...
# or OPENAI_API_KEY=...

# Mail (optional)
MAIL_HOST=smtp.example.com
MAIL_USER=...
MAIL_PASSWORD=...
MAIL_FROM=noreply@example.com

# Public URLs
PUBLIC_APP_URL=https://app.example.com
API_PUBLIC_URL=https://app.example.com/api/v1
```

Generate secrets:
```bash
openssl rand -base64 48
```

---

## 5. Docker Images

### Backend Dockerfile (multi-stage)
1. `maven:3.9-eclipse-temurin-21` → `./mvnw -DskipTests package`
2. `eclipse-temurin:21-jre` → copy jar, run as non-root user

### Frontend Dockerfile
1. `node:20-alpine` → `npm ci && npm run build`
2. Copy `dist/` into Nginx image **or** shared volume consumed by edge Nginx

Healthcheck API: `GET /actuator/health`.

---

## 6. Compose Services (prod profile)

```yaml
services:
  postgres:
    image: postgres:16
    volumes: [pgdata:/var/lib/postgresql/data]
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    restart: unless-stopped

  api:
    build: ./backend
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${DB_NAME}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      AI_PROVIDER: ${AI_PROVIDER}
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      CORS_ORIGINS: ${CORS_ORIGINS}
    depends_on: [postgres]
    restart: unless-stopped

  nginx:
    image: nginx:1.27-alpine
    ports: ["80:80", "443:443"]
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - frontend_dist:/usr/share/nginx/html:ro
      - certbot_etc:/etc/letsencrypt:ro
    depends_on: [api]
    restart: unless-stopped
```

---

## 7. Nginx Sketch

```nginx
server {
  listen 443 ssl http2;
  server_name app.example.com;

  # ssl_certificate ...;
  # ssl_certificate_key ...;

  root /usr/share/nginx/html;
  index index.html;

  location /api/ {
    proxy_pass http://api:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Request-Id $request_id;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 120s;  # AI calls
  }

  location /actuator/health {
    proxy_pass http://api:8080/actuator/health;
  }

  location / {
    try_files $uri /index.html;
  }
}
```

Disable Swagger UI in prod (`springdoc.swagger-ui.enabled=false`).

---

## 8. Deploy Steps

1. Clone release tag on VM.  
2. Create `.env` from example; fill secrets.  
3. `docker compose -f docker-compose.yml -f docker-compose.prod.yml build`  
4. `docker compose ... up -d`  
5. Verify: `curl -f https://app.example.com/actuator/health`  
6. Run smoke: register user via UI.  
7. Confirm Flyway version in logs (`Successfully applied`).

**Local dry-run (before first VM deploy):**
```bash
./scripts/deploy-dry-run.sh
```
Builds `docker-compose.yml` + `docker-compose.prod.yml` + dedicated job worker overlay, probes edge health on `http://localhost:8090`, verifies worker health, then tears down. CI runs the same script on every push/PR.

### Background job worker (production)

Async jobs (knowledge reindex, document generate) are queued in Postgres. By default the API **does not** poll in `prod` profile (`aistudio.jobs.worker-enabled=false`). Run a dedicated worker container:

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  -f docker-compose.worker.yml \
  -f docker-compose.worker-prod.yml \
  up -d
```

Staging GHCR deploy includes the worker automatically (`scripts/staging-ghcr-deploy.sh`).

| Variable | API (prod) | Worker |
|---|---|---|
| `AISTUDIO_JOBS_WORKER_ENABLED` | `false` (via compose overlay) | `true` |
| `SPRING_PROFILES_ACTIVE` | `prod` | `prod,worker` |

Worker uses the same API image, no public port. Health: `docker compose exec worker curl -fsS http://127.0.0.1:8080/actuator/health`.

Local dev (`docker compose up`) keeps in-process polling enabled on the API for simplicity.

### Zero-downtime (MVP lite)
- `docker compose up -d --no-deps --build api` then reload nginx.
- Prefer maintenance window for first betas.

---

## 9. Database Migrations

- Migrations run on API startup via Flyway.
- **Backup before deploy:**
```bash
docker compose exec -T postgres pg_dump -U aistudio aistudio > backup-$(date +%F).sql
```
- Forward-only migrations; test on staging first.

---

## 10. CI/CD (GitHub Actions)

**On PR / push:** `ci.yml` — backend tests, frontend Vitest + build, Playwright E2E (compose).  
**Publish:** `publish.yml` builds `api` and `frontend` images and pushes to GHCR on `main` / `v*` tags (PR builds without push).

Images:
- `ghcr.io/<owner>/<repo>/api:<tag>`
- `ghcr.io/<owner>/<repo>/frontend:<tag>`

Tags include branch name (`main`), git tag ref (`v0.1.0-beta`), semver (`0.1.0-beta` from `v0.1.0-beta`), and `sha-<short>`.

Never store provider keys in workflow logs; use GitHub Secrets.

### Staging deploy (GHCR images)

```bash
cp .env.example .env   # set JWT_SECRET, DB_PASSWORD, CORS_ORIGINS
export IMAGE_TAG=main  # or sha-... / v0.2.2-beta / 0.2.2-beta
docker compose -f docker-compose.yml -f docker-compose.staging.yml pull
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d
./scripts/healthcheck.sh http://localhost:8088
```

GHCR packages may be private — `docker login ghcr.io` with a PAT that has `read:packages`.

**Local validation (no GHCR):** `./scripts/staging-dry-run.sh` builds images and boots `docker-compose.yml` + `docker-compose.staging.yml` + `docker-compose.staging-local.yml` on non-default ports (`8091` UI, `8092` API), runs `healthcheck.sh`, then tears down. CI runs the same script on every push/PR.

**GHCR staging host deploy:**

```bash
cp .env.example .env   # set JWT_SECRET, DB_PASSWORD, CORS_ORIGINS
./scripts/validate-staging-env.sh
export IMAGE_TAG=main  # or sha-... / v0.2.2-beta / 0.2.2-beta
docker login ghcr.io   # if packages are private
./scripts/staging-ghcr-deploy.sh
```

### Stripe billing (production)

Set `BILLING_PROVIDER=stripe` and Stripe secrets in `.env` (never commit):

| Variable | Purpose |
|---|---|
| `STRIPE_API_KEY` | Secret key (`sk_live_…` or `sk_test_…`) |
| `STRIPE_WEBHOOK_SECRET` | Signing secret from Stripe Dashboard webhook endpoint |
| `STRIPE_PRO_PRICE_ID` | Price ID for Pro plan |
| `STRIPE_TEAM_PRICE_ID` | Price ID for Team plan |

Webhook URL (public HTTPS): `https://<api-host>/api/v1/billing/stripe/webhook`

Subscribe to `checkout.session.completed`, `customer.subscription.updated`, and `customer.subscription.deleted`.

Optional: set `plans.stripe_price_id` in the database (migration `V13`) instead of env price IDs.

Default `BILLING_PROVIDER=mock` keeps mock checkout for local dev and CI.

### OIDC SSO (production)

Set `SSO_PROVIDER=oidc` and configure the IdP (Okta, Google Workspace, Azure AD, etc.):

| Variable | Purpose |
|---|---|
| `OIDC_ISSUER_URI` | Issuer URL (discovery at `/.well-known/openid-configuration`) |
| `OIDC_CLIENT_ID` | OAuth client ID |
| `OIDC_CLIENT_SECRET` | OAuth client secret |
| `OIDC_DISPLAY_NAME` | Button label in login UI (optional) |
| `OIDC_SCOPES` | Defaults to `openid email profile` |

Register redirect URI: `https://<app-host>/auth/sso/callback`

Default `SSO_PROVIDER=mock` for local dev and CI E2E.

### Cloud Agent environment builds

Repository config lives in `.cursor/environment.json` (install, start, dev-server terminals). After merging to `main`:

1. Open **Cursor → Cloud Agents → Environment** for this repo.
2. Confirm install/start match `.cursor/install.sh` and `.cursor/start.sh`.
3. **Builds** tab → trigger a build from `main` (promotable only from the default branch).
4. When the build succeeds, **Save** so new agents boot from the snapshot.

Draft builds from feature branches validate install but cannot be promoted — merge first, then build `main`.

---

## 11. Observability (MVP)

- `docker compose logs -f api` — **prod profile emits JSON** logs (Logstash encoder) with `requestId` from `X-Request-Id` / MDC
- Ship JSON stdout to Loki/ELK when ready (no agent required in-repo)
- Uptime: `./scripts/healthcheck.sh https://staging.example.com` (edge `/actuator/health` + SPA)
- Post-deploy: `./scripts/post-deploy-smoke.sh https://staging.example.com` (health, info, confirms prometheus is not on the public edge)
- **Dogfood:** `./scripts/staging-dogfood.sh https://staging.example.com` (env validation + health + smoke + optional internal metrics). Full Stripe/OIDC/manual checklist: [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md)
- **Prometheus:** `GET /actuator/prometheus` on the API (authenticated; not proxied by nginx). Scrape from the internal Docker network or VPN, not the public hostname.
- **Monitoring overlay:** `docker-compose.monitoring.yml` + `monitoring/README.md` (Grafana dashboard, Prometheus, Alertmanager, Loki logs, `METRICS_SCRAPE_TOKEN`)
- Disk alerts for Postgres volume (host/ops)

---

## 12. Backup & Restore

**Backup:** `./scripts/backup-db.sh` (gzipped `pg_dump`; schedule nightly via cron to object storage).  
**Restore:** `./scripts/restore-db.sh ./backups/aistudio-….sql.gz` (requires typing `YES`).  
Test restore on staging quarterly.

---

## 13. Security Hardening Checklist

- [x] TLS-ready Nginx config (enable certs for production)
- [x] Strong `JWT_SECRET` and DB password required in prod compose overlay
- [x] CORS limited via `CORS_ORIGINS`
- [x] Swagger disabled in `prod` Spring profile
- [x] Non-root API container user
- [ ] Firewall: only 80/443 public (operator responsibility)
- [x] Rate limiting enabled
- [ ] OS packages updated (operator responsibility)

---

## 14. Local Dev vs Prod

| Concern | Dev | Prod |
|---|---|---|
| AI | mock default | real provider |
| Mail | logging adapter | SMTP |
| TLS | optional | required |
| Compose | hot reload mounts optional | immutable images |

---

## 15. Rollback

1. `docker compose` point API to previous image tag.  
2. If migration is incompatible, restore DB backup taken pre-deploy (plan migrations to be backward compatible when possible).

---

## 16. Prototype Note

The existing FastAPI prototype is **not** the production deploy target. Keep it for demos until Spring/React MVP replaces it. Do not expose the prototype publicly without auth.

---

## 17. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Compose + Nginx MVP deploy guide |

**Previous:** `12-TESTING-STRATEGY.md` · **Index:** `docs/README.md`
