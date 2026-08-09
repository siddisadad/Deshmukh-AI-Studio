# SSO multi-IdP and metadata refresh automation

**Version:** v0.2.52-beta  
**Scope:** Per-organization multiple OIDC/SAML IdPs, automated metadata refresh, and login discovery by org slug.

Complements OIDC guides ([15-OIDC-IDP-GUIDE.md](15-OIDC-IDP-GUIDE.md)) and SAML SP binding ([20-SAML-SP-BINDING-GUIDE.md](20-SAML-SP-BINDING-GUIDE.md)).

---

## Database (V29)

Table `organization_sso_idps` stores per-org IdP definitions:

| Column | Purpose |
|--------|---------|
| `slug` | Unique per org; used in settings UI |
| `protocol` | `OIDC` or `SAML` |
| `display_name` | Login button label |
| OIDC fields | `issuer_uri`, `client_id`, `client_secret`, `scopes` |
| SAML fields | `metadata_url`, `entity_id`, `acs_url`, SP keys |
| `metadata_json` | Cached discovery / federation metadata |
| `metadata_fetched_at` | Last successful refresh |
| `metadata_refresh_error` | Last refresh failure message |

Provider id for login/API: `db-{idpUuid}`.

---

## API

### Organization settings (OWNER)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/organizations/{orgId}/sso/idps` | List IdPs |
| `POST` | `/organizations/{orgId}/sso/idps` | Create IdP (auto metadata refresh) |
| `PUT` | `/organizations/{orgId}/sso/idps/{idpId}` | Update IdP |
| `DELETE` | `/organizations/{orgId}/sso/idps/{idpId}` | Remove IdP |
| `POST` | `/organizations/{orgId}/sso/idps/{idpId}/refresh-metadata` | Manual metadata refresh |

### Login discovery

`GET /api/v1/auth/sso/providers?organizationSlug={slug}` — lists env-configured providers plus enabled org IdPs.

Sign-in page: `/login?org={orgSlug}` shows org-specific IdP buttons.

SSO start/callback use provider id `db-{uuid}` for configured IdPs.

---

## Metadata refresh automation

| Env var | Default | Purpose |
|---------|---------|---------|
| `SSO_METADATA_REFRESH_ENABLED` | `false` | Enable scheduler |
| `SSO_METADATA_REFRESH_INTERVAL_MS` | `3600000` | Scheduler interval (1h) |

`SsoMetadataRefreshScheduler` refreshes all enabled IdPs:

- **OIDC** — fetches `/.well-known/openid-configuration`
- **SAML** — fetches federation metadata XML

Manual refresh via settings UI or API.

---

## UI

`/settings/sso` — list IdPs, add OIDC/SAML configs, refresh metadata, remove IdPs.

---

## Staging smoke

```bash
export IMAGE_TAG=v0.2.52-beta
export SSO_METADATA_REFRESH_ENABLED=true
# Configure IdP in /settings/sso, then:
curl -s "$API_URL/api/v1/auth/sso/providers?organizationSlug=your-org-slug"
```
