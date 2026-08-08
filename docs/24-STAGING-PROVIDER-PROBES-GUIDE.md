# Staging provider probes guide

**Version:** v0.2.19-beta  
**Script:** `scripts/staging-provider-probes.sh`  
**Parent playbook:** [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md)

Automated readiness checks for **real** Stripe, OIDC, SAML, and SMTP configuration before inviting beta users on a staging host.

---

## What it checks

| Provider | When | Probe |
|----------|------|--------|
| **All** | Always | `GET /auth/sso/providers`, `GET /billing/plans` |
| **Mock SSO** | `SSO_PROVIDER=mock` | `POST /auth/sso/start` (mock) returns `authorizationUrl` |
| **OIDC** | `SSO_PROVIDER=oidc` | Issuer discovery JSON + issuer match; SSO start returns IdP URL |
| **SAML SP** | `SSO_PROVIDER=saml`, `SAML_STUB_MODE=false` | IdP metadata URL; SP metadata XML; SSO start |
| **SAML stub** | `SSO_PROVIDER=saml`, `SAML_STUB_MODE=true` | SSO start (stub redirect) |
| **Stripe** | `BILLING_PROVIDER=stripe` | Stripe API validates `STRIPE_PRO_PRICE_ID` and `STRIPE_TEAM_PRICE_ID` |
| **SMTP** | `MAIL_PROVIDER=smtp` | TCP connect to `MAIL_HOST:MAIL_PORT` |
| **Loki regions** | `LOKI_QUERY_REGIONS` set | Each regional Loki `/ready` |

Mock billing skips Stripe API calls. Logging mail skips SMTP connect.

---

## Usage

After deploy and `validate-staging-env.sh`:

```bash
./scripts/staging-provider-probes.sh https://staging.yourdomain.com
```

Direct API (when edge does not proxy or for SAML metadata on API host):

```bash
export API_URL=http://localhost:8080   # or internal API URL
./scripts/staging-provider-probes.sh https://staging.yourdomain.com
```

Integrated into `staging-dogfood.sh` as step **5/7** and `staging-dry-run.sh` (mock providers).

---

## Environment

Uses the same `.env` as staging deploy. `SSO_APP_BASE_URL` (or edge URL) builds `redirectUri` for SSO start probes: `{base}/auth/sso/callback`.

OIDC issuer must match discovery document (`OIDC_ISSUER_URI` without trailing-slash mismatch).

---

## Staging smoke

```bash
export IMAGE_TAG=v0.2.19-beta
./scripts/staging-ghcr-deploy.sh
./scripts/staging-dogfood.sh https://staging.yourdomain.com
```

Expect step 5 **staging-provider-probes** to pass before manual Stripe/OIDC sign-off.

---

## Troubleshooting

| Failure | Fix |
|---------|-----|
| OIDC issuer mismatch | Align `OIDC_ISSUER_URI` with discovery `issuer` (trailing slash) |
| Stripe price 404 | Wrong test-mode key or price ID from Dashboard |
| SAML SP metadata 404 | `SAML_STUB_MODE=false` and API running with SAML SP adapter |
| SMTP connect failed | Firewall, wrong host/port, or TLS-only port without STARTTLS relay |

---

**Previous:** [23-STREAMING-TOKEN-UX-GUIDE.md](23-STREAMING-TOKEN-UX-GUIDE.md) · **Next:** [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md)
