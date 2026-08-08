# OIDC IdP setup guide (staging / production)

Step-by-step OAuth/OIDC application setup for **AI Studio** with common identity providers. Use alongside [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) §6 for the manual sign-off checklist.

**MVP scope:** OIDC only (authorization code flow). Full SAML is not supported — use the dev stub ([16-SAML-SSO-STUB.md](16-SAML-SSO-STUB.md)) or configure your IdP as OIDC/OAuth2.

---

## 1. Shared requirements (all IdPs)

| Setting | Value |
|---|---|
| Application type | Web / OIDC (authorization code) |
| Redirect URI (callback) | `https://<your-host>/auth/sso/callback` |
| Scopes | `openid email profile` (default in app) |
| Sign-in response | Code (not implicit) |

The SPA callback route is `/auth/sso/callback` (see `frontend/src/app/router/routes.tsx`). The API exchanges the code server-side via `OidcSsoAdapter`.

### 1.1 Environment variables

```bash
SSO_ENABLED=true
SSO_PROVIDER=oidc
SSO_APP_BASE_URL=https://staging.yourdomain.com
OIDC_ISSUER_URI=<issuer — see IdP sections below>
OIDC_CLIENT_ID=<client id>
OIDC_CLIENT_SECRET=<client secret>
OIDC_DISPLAY_NAME=Continue with SSO
# Optional override (default openid email profile):
# OIDC_SCOPES=openid email profile
```

Also set:

```bash
CORS_ORIGINS=https://staging.yourdomain.com
BILLING_APP_BASE_URL=https://staging.yourdomain.com
```

Redeploy the API after changing SSO env vars (`docker compose up -d` or staging GHCR script).

### 1.2 Verify discovery

```bash
curl -fsS "$OIDC_ISSUER_URI/.well-known/openid-configuration" | jq .issuer,.authorization_endpoint,.token_endpoint,.userinfo_endpoint
```

Issuer in the JSON must match `OIDC_ISSUER_URI` (no trailing-slash mismatch).

### 1.3 Smoke test

1. Open `https://<your-host>/login`
2. Click the SSO button (`OIDC_DISPLAY_NAME`)
3. Complete IdP login → land on `/dashboard`
4. Log out → SSO again → same user id

---

## 2. Okta

### 2.1 Create application

1. Okta Admin → **Applications** → **Create App Integration**
2. Sign-in method: **OIDC - OpenID Connect**
3. Application type: **Web Application**
4. **Grant type:** Authorization Code (refresh optional)
5. **Sign-in redirect URIs:** `https://staging.yourdomain.com/auth/sso/callback`
6. **Sign-out redirect URIs:** optional (`https://staging.yourdomain.com/login`)
7. Assign to users/groups for staging

### 2.2 Issuer and credentials

- **Issuer:** `https://{yourOktaDomain}/oauth2/default` (default authorization server)
  - Or a custom authorization server: `https://{yourOktaDomain}/oauth2/{authServerId}`
- Copy **Client ID** and **Client secret** from the app **General** tab

```bash
OIDC_ISSUER_URI=https://dev-12345678.okta.com/oauth2/default
OIDC_CLIENT_ID=0oa...
OIDC_CLIENT_SECRET=...
OIDC_DISPLAY_NAME=Continue with Okta
```

### 2.3 Okta pitfalls

| Issue | Fix |
|---|---|
| `redirect_uri` mismatch | URI must match exactly (HTTPS, no trailing slash on path) |
| Missing email | Ensure `openid email profile` scopes; check Okta app **OpenID Connect ID Token** claims include `email` |
| Wrong issuer | Use the authorization server issuer from **Security → API → Authorization Servers** |

---

## 3. Microsoft Entra ID (Azure AD)

### 3.1 Register application

1. Azure Portal → **Microsoft Entra ID** → **App registrations** → **New registration**
2. Name: `AI Studio Staging`
3. Supported account types: per your tenant policy (single tenant is typical for staging)
4. Redirect URI: **Web** → `https://staging.yourdomain.com/auth/sso/callback`
5. Register → note **Application (client) ID**

### 3.2 Client secret

1. **Certificates & secrets** → **New client secret**
2. Copy secret value immediately (shown once)

### 3.3 Token configuration

1. **Token configuration** → **Add optional claim** → ID token → `email`, `profile` (if not present)
2. **API permissions:** `openid`, `email`, `profile` (Microsoft Graph delegated) — grant admin consent if required

### 3.4 Issuer and env

Use the **v2.0** issuer for multi-tenant or single-tenant:

```bash
OIDC_ISSUER_URI=https://login.microsoftonline.com/{tenant-id}/v2.0
OIDC_CLIENT_ID={application-client-id}
OIDC_CLIENT_SECRET={client-secret-value}
OIDC_DISPLAY_NAME=Continue with Microsoft
```

Replace `{tenant-id}` with Directory (tenant) ID from app **Overview**.

### 3.5 Azure pitfalls

| Issue | Fix |
|---|---|
| `AADSTS50011` redirect URI | Add exact callback URL under **Authentication → Web redirect URIs** |
| Missing email in userinfo | Add optional claims; ensure user has UPN/email in directory |
| Issuer validation fails | Use `.../v2.0` issuer, not legacy `.../` without v2 |

---

## 4. Google (Workspace or Cloud Identity)

Google uses the Google identity issuer; create an OAuth client in Google Cloud Console.

### 4.1 OAuth client

1. [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services** → **Credentials**
2. **Create Credentials** → **OAuth client ID**
3. Application type: **Web application**
4. **Authorized redirect URIs:** `https://staging.yourdomain.com/auth/sso/callback`
5. Copy Client ID and Client secret

### 4.2 OAuth consent screen

Configure consent screen (Internal for Workspace-only, External for broader testing). Add scopes: `openid`, `email`, `profile`.

### 4.3 Env

```bash
OIDC_ISSUER_URI=https://accounts.google.com
OIDC_CLIENT_ID=....apps.googleusercontent.com
OIDC_CLIENT_SECRET=GOCSPX-...
OIDC_DISPLAY_NAME=Continue with Google
```

### 4.4 Google pitfalls

| Issue | Fix |
|---|---|
| `redirect_uri_mismatch` | Redirect URI in console must match staging host exactly |
| `access_denied` | User not in test users list (External app in Testing mode) |
| No email | Ensure `email` scope; Workspace users need primary email set |

---

## 5. Auth0

### 5.1 Create application

1. Auth0 Dashboard → **Applications** → **Create Application**
2. Type: **Regular Web Application** (not SPA — secret is used server-side)
3. **Settings → Application URIs:**
   - **Allowed Callback URLs:** `https://staging.yourdomain.com/auth/sso/callback`
   - **Allowed Logout URLs:** `https://staging.yourdomain.com/login` (optional)
4. **Advanced → OAuth** → OIDC Conformant: enabled (default on new tenants)

### 5.2 Env

```bash
OIDC_ISSUER_URI=https://{your-tenant}.auth0.com/
OIDC_CLIENT_ID=...
OIDC_CLIENT_SECRET=...
OIDC_DISPLAY_NAME=Continue with Auth0
```

Issuer is shown on the application **Settings** tab (Domain + trailing slash per Auth0 convention). Verify with discovery URL above.

### 5.3 Auth0 pitfalls

| Issue | Fix |
|---|---|
| `Callback URL mismatch` | Add staging URL to Allowed Callback URLs |
| SPA vs Regular Web | Use **Regular Web Application** so client secret works |
| Email missing | Auth0 Rules/Actions must not strip `email` from user profile |

---

## 6. Troubleshooting (all providers)

| Symptom | Likely cause |
|---|---|
| Login button missing | `SSO_ENABLED=false` or `SSO_PROVIDER` not `oidc`; check API logs at startup |
| Redirect to IdP then error page | API logs: `INVALID_TOKEN`, state expiry (10 min), or code reuse |
| `OIDC userinfo missing email` | IdP not returning `email`; fix scopes/claims |
| CORS errors after login | `CORS_ORIGINS` must include staging HTTPS origin |
| Works on localhost but not staging | Staging requires HTTPS origins; mock SSO is default in `.env.example` |

---

## 7. Document control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-08 | Post v0.2.7-beta IdP playbooks for staging dogfood |

**Previous:** [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) · **Next:** [09-DEVELOPMENT-ROADMAP.md](09-DEVELOPMENT-ROADMAP.md)
