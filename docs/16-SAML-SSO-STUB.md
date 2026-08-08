# SAML SSO stub (dev / CI)

**Production IdPs:** use OIDC — [15-OIDC-IDP-GUIDE.md](15-OIDC-IDP-GUIDE.md). Most enterprise IdPs (Okta, Entra ID, Google Workspace, Auth0) support OIDC alongside SAML.

**MVP scope:** full SAML SP-initiated login (HTTP-POST `SAMLResponse` binding) is **not implemented**. This document covers the **stub adapter** for local dev, CI, and wiring validation.

---

## 1. What the stub does

| Setting | Behavior |
|---|---|
| `SSO_PROVIDER=saml` + `SAML_STUB_MODE=true` (default) | Dev/CI stub — redirect callback with mock `code` (like `SSO_PROVIDER=mock`) |
| `SSO_PROVIDER=saml` + `SAML_STUB_MODE=false` | **Startup fails** — full SAML not shipped; use OIDC or keep stub mode |

The stub implements `SsoPort` via `SamlSsoAdapter` so the login UI, `/auth/sso/callback`, and `SsoService` paths are exercised without an IdP.

---

## 2. Environment variables (stub mode)

```bash
SSO_ENABLED=true
SSO_PROVIDER=saml
SSO_APP_BASE_URL=http://localhost:5173
SAML_STUB_MODE=true
SAML_DISPLAY_NAME=Continue with SAML (stub)
```

Optional (required only when `SAML_STUB_MODE=false` — will still fail until full SAML ships):

```bash
SAML_METADATA_URL=https://idp.example.com/metadata
SAML_ENTITY_ID=https://your-app.example.com/saml/metadata
```

Also set `CORS_ORIGINS` and `BILLING_APP_BASE_URL` to your app origin (same as OIDC).

---

## 3. Smoke test (local)

1. Set env above (or `SSO_PROVIDER=saml` in `.env`)
2. Start API + frontend
3. Open `/login` → click **Continue with SAML (stub)**
4. Land on `/dashboard` as a new SAML stub user

---

## 4. Future full SAML (backlog)

Planned depth (not in MVP):

- SP metadata endpoint + signed `AuthnRequest`
- HTTP-POST callback parsing `SAMLResponse`
- Attribute mapping (`email`, `displayName`) to `SsoPort.UserInfo`
- IdP metadata refresh from `SAML_METADATA_URL`

Track in [09-DEVELOPMENT-ROADMAP.md](09-DEVELOPMENT-ROADMAP.md).

---

## Document control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-08 | SAML port stub + env validation |
