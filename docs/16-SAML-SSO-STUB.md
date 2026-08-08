# SAML SSO stub (dev / CI)

**Production IdPs:** use OIDC — [15-OIDC-IDP-GUIDE.md](15-OIDC-IDP-GUIDE.md). For SAML enterprises, use SP binding — [20-SAML-SP-BINDING-GUIDE.md](20-SAML-SP-BINDING-GUIDE.md).

**MVP scope:** this document covers the **stub adapter** for local dev, CI, and wiring validation. Set `SAML_STUB_MODE=false` for real SP-initiated SAML.

---

## 1. What the stub does

| Setting | Behavior |
|---|---|
| `SSO_PROVIDER=saml` + `SAML_STUB_MODE=true` (default) | Dev/CI stub — redirect callback with mock `code` (like `SSO_PROVIDER=mock`) |
| `SSO_PROVIDER=saml` + `SAML_STUB_MODE=false` | Real SP-initiated SAML — see [20-SAML-SP-BINDING-GUIDE.md](20-SAML-SP-BINDING-GUIDE.md) |

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

Optional (required when `SAML_STUB_MODE=false`):

```bash
SAML_METADATA_URL=https://idp.example.com/metadata
SAML_ENTITY_ID=https://your-app.example.com/saml/metadata
SAML_ACS_URL=https://your-app.example.com/api/v1/auth/sso/saml/acs
```

Also set `CORS_ORIGINS` and `BILLING_APP_BASE_URL` to your app origin (same as OIDC).

---

## 3. Smoke test (local)

1. Set env above (or `SSO_PROVIDER=saml` in `.env`)
2. Start API + frontend
3. Open `/login` → click **Continue with SAML (stub)**
4. Land on `/dashboard` as a new SAML stub user

---

## 4. Full SAML SP binding

Shipped in **v0.2.15-beta** — [20-SAML-SP-BINDING-GUIDE.md](20-SAML-SP-BINDING-GUIDE.md).

---

## Document control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-08 | SAML port stub + env validation |
