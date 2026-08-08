# Staging Dogfood Guide

Operator playbook for validating **AI Studio** on a real HTTPS staging host before inviting beta users. Combines automated gates (`scripts/staging-dogfood.sh`) with manual flows for billing, SSO, mail, and observability.

**Prerequisites:** [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) (compose overlays, GHCR deploy, Stripe/OIDC env vars).

---

## 1. Goals

| Gate | What it proves |
|---|---|
| Env validation | Prod-shaped secrets, HTTPS CORS, provider config consistency |
| Health + smoke | Edge SPA, API health/info, no public Prometheus leak |
| API smoke | Register → org → project journey over the public URL |
| Internal metrics | Optional Prometheus scrape with `METRICS_SCRAPE_TOKEN` |
| Manual dogfood | Real UX: AI chat (SSE reconnect), RAG, billing, SSO, mail |

---

## 2. One-time staging setup

### 2.1 Host and TLS

1. Provision a VM or PaaS host with Docker Compose v2.
2. Point DNS `staging.yourdomain.com` to the host.
3. Terminate TLS at nginx (or your edge). The app expects **HTTPS** in `CORS_ORIGINS` when `SPRING_PROFILES_ACTIVE=prod`.

### 2.2 Environment file

```bash
cp .env.example .env
```

Set at minimum:

| Variable | Staging example |
|---|---|
| `JWT_SECRET` | 32+ random bytes (`openssl rand -hex 32`) |
| `DB_PASSWORD` | Strong password (not `aistudio`) |
| `CORS_ORIGINS` | `https://staging.yourdomain.com` |
| `BILLING_APP_BASE_URL` | `https://staging.yourdomain.com` |
| `SSO_APP_BASE_URL` | `https://staging.yourdomain.com` |
| `SPRING_PROFILES_ACTIVE` | `prod` |

Validate before deploy:

```bash
./scripts/validate-staging-env.sh
```

### 2.3 Deploy from GHCR

Published images: `ghcr.io/siddisadad/deshmukh-ai-studio/api` and `frontend` (tags: `main`, `v0.2.0-beta`, `sha-…`).

```bash
docker login ghcr.io   # if packages are private
export IMAGE_TAG=v0.2.0-beta
./scripts/staging-ghcr-deploy.sh
```

Confirm edge health:

```bash
./scripts/healthcheck.sh https://staging.yourdomain.com
```

---

## 3. Automated dogfood gates

Run after every deploy or `.env` change:

```bash
./scripts/staging-dogfood.sh https://staging.yourdomain.com
```

With internal metrics (recommended on staging):

```bash
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh
# Recreate API so it picks up the token (or set in .env and redeploy)
docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d api

export API_URL=http://localhost:8080   # direct API, not nginx edge
./scripts/staging-dogfood.sh https://staging.yourdomain.com
```

The script runs:

1. `validate-staging-env.sh`
2. `healthcheck.sh` (edge)
3. `post-deploy-smoke.sh` (edge; confirms `/actuator/prometheus` is **not** on the public hostname)
4. `api-smoke.sh` (authenticated API journey)
5. Optional internal `/actuator/prometheus` check
6. Prints manual checklist items based on `BILLING_PROVIDER`, `SSO_PROVIDER`, and `MAIL_PROVIDER`

CI runs `bash -n` on these scripts and `staging-dry-run.sh` invokes `api-smoke.sh` locally.

---

## 4. Provider modes

Default `.env.example` uses **mock** billing and SSO — fine for infra smoke, not for billing/SSO dogfood.

| Concern | Mock (CI / quick smoke) | Real dogfood |
|---|---|---|
| Billing | `BILLING_PROVIDER=mock` | `BILLING_PROVIDER=stripe` + test keys |
| SSO | `SSO_PROVIDER=mock` | `SSO_PROVIDER=oidc` + IdP app |
| Mail | `MAIL_PROVIDER=logging` | `MAIL_PROVIDER=smtp` |

Toggle providers in `.env`, redeploy API (`docker compose … up -d api`), then re-run `staging-dogfood.sh`.

---

## 5. Stripe test-mode dogfood

### 5.1 Stripe Dashboard

1. Use a **Test mode** Stripe account.
2. Create Products/Prices for Pro and Team (or use existing test price IDs).
3. Developers → Webhooks → Add endpoint:
   - URL: `https://staging.yourdomain.com/api/v1/billing/stripe/webhook`
   - Events: `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`
4. Copy signing secret → `STRIPE_WEBHOOK_SECRET`.

### 5.2 `.env`

```bash
BILLING_PROVIDER=stripe
STRIPE_API_KEY=sk_test_…
STRIPE_WEBHOOK_SECRET=whsec_…
STRIPE_PRO_PRICE_ID=price_…
STRIPE_TEAM_PRICE_ID=price_…
BILLING_APP_BASE_URL=https://staging.yourdomain.com
```

Alternatively set `plans.stripe_price_id` in the database (Flyway `V13`) instead of env price IDs.

### 5.3 Manual checklist

- [ ] Settings → Billing shows current plan and upgrade paths
- [ ] Checkout redirects to Stripe Hosted Checkout; complete with test card `4242 4242 4242 4242`
- [ ] Return URL lands on app; plan reflects Pro or Team
- [ ] Customer portal opens from billing settings
- [ ] Stripe Dashboard → Webhooks shows **200** deliveries for test events
- [ ] Cancel subscription in portal; app downgrades or shows expected state

### 5.4 Troubleshooting

| Symptom | Check |
|---|---|
| Checkout 500 | API logs; `STRIPE_API_KEY` and price IDs |
| Webhook 401/400 | `STRIPE_WEBHOOK_SECRET`; URL must be HTTPS and match nginx route to API |
| Plan not updating | Webhook events subscribed; API logs for `StripeWebhookController` |

---

## 6. OIDC SSO dogfood

### 6.1 IdP application

Create an OAuth/OIDC app in your IdP (Okta, Google Workspace, Azure AD, Auth0, etc.):

| Setting | Value |
|---|---|
| Redirect URI | `https://staging.yourdomain.com/auth/sso/callback` |
| Scopes | `openid email profile` (default) |

Note the issuer URL, client ID, and client secret.

### 6.2 `.env`

```bash
SSO_ENABLED=true
SSO_PROVIDER=oidc
SSO_APP_BASE_URL=https://staging.yourdomain.com
OIDC_ISSUER_URI=https://your-idp.example.com/oauth2/default
OIDC_CLIENT_ID=…
OIDC_CLIENT_SECRET=…
OIDC_DISPLAY_NAME=Continue with SSO
```

Redeploy API after changes.

### 6.3 Manual checklist

- [ ] Login page shows SSO button with `OIDC_DISPLAY_NAME`
- [ ] Clicking starts redirect to IdP authorize URL
- [ ] After login, callback creates or links user and lands on dashboard
- [ ] Logout and SSO login again — same user, org/project access intact
- [ ] User without org gets onboarding path (not a blank error)

### 6.4 IdP-specific notes

| IdP | Issuer tip |
|---|---|
| Okta | Issuer like `https://{org}.okta.com/oauth2/default` |
| Azure AD | Issuer `https://login.microsoftonline.com/{tenant}/v2.0` |
| Google | Issuer `https://accounts.google.com`; restrict OAuth client to staging host |
| Auth0 | Issuer `https://{tenant}.auth0.com/` |

Full SAML is not in MVP; use OIDC or mock SSO for staging.

### 6.5 Troubleshooting

| Symptom | Check |
|---|---|
| redirect_uri mismatch | IdP app redirect exactly matches `/auth/sso/callback` on staging host |
| Invalid issuer | `OIDC_ISSUER_URI` reachable; discovery at `/.well-known/openid-configuration` |
| User created but no email | IdP must return `email` scope; check userinfo in API logs |

---

## 7. SMTP mail (password reset)

For forgot-password dogfood:

```bash
MAIL_PROVIDER=smtp
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USER=…
MAIL_PASSWORD=…
MAIL_FROM=noreply@staging.yourdomain.com
```

Mail health is disabled by default in prod profile (`management.health.mail.enabled=false`). Trigger forgot-password from login UI and confirm delivery (or provider logs).

---

## 8. Monitoring overlay (optional)

```bash
export METRICS_SCRAPE_TOKEN="$(openssl rand -hex 32)"
./scripts/write-prometheus-token.sh
docker compose \
  -f docker-compose.yml \
  -f docker-compose.staging.yml \
  -f docker-compose.monitoring.yml \
  up -d
```

- Grafana: `http://localhost:3000` (do not expose publicly without auth)
- Prometheus: `http://localhost:9090` (internal only)
- Dashboard: `monitoring/grafana/dashboard.json`
- Alerts: `monitoring/alerts.yml` (wire Alertmanager for paging)

See [monitoring/README.md](../monitoring/README.md).

---

## 9. Full manual UX checklist

Run as an operator or friendly beta user after automated gates pass.

### Identity and tenancy

- [ ] Register new account (email/password)
- [ ] Login, logout, refresh session (SPA reload stays authenticated)
- [ ] Forgot / reset password (if SMTP enabled)
- [ ] Create organization and project

### Work management

- [ ] Requirements CRUD and editor
- [ ] Tasks, labels, Kanban drag
- [ ] Documents CRUD and markdown preview

### AI workspace

- [ ] Open each assistant type; send messages
- [ ] Multi-thread: switch threads; history persists
- [ ] Streaming: watch tokens arrive; **disconnect network briefly** — UI should reconnect or recover via polling
- [ ] Cancel in-flight generation
- [ ] BA actions (improve requirement, stories, AC) if exposed in UI

### RAG

- [ ] Upload or index a document for the project
- [ ] Search project context; results appear in chat context

### Billing / SSO (when enabled)

- [ ] Sections 5.3 and 6.3

### Plugins (if enabled for org)

- [ ] Org plugin toggles persist; feature surfaces as expected

---

## 10. Sign-off

Record for each staging release:

| Field | Example |
|---|---|
| Date | 2026-08-08 |
| `IMAGE_TAG` | `v0.2.0-beta` |
| Host | `https://staging.yourdomain.com` |
| Providers | stripe / oidc / smtp / mock |
| Automated | `staging-dogfood.sh` exit 0 |
| Manual | Checklist §9 complete |
| Issues | Link to GitHub issues |

When sign-off is green, tag the next beta on `main` and update [CHANGELOG.md](../CHANGELOG.md).

---

## 11. Document control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-08 | Post v0.2.0-beta staging dogfood playbook |

**Previous:** [13-DEPLOYMENT-GUIDE.md](13-DEPLOYMENT-GUIDE.md) · **Next:** [09-DEVELOPMENT-ROADMAP.md](09-DEVELOPMENT-ROADMAP.md)
