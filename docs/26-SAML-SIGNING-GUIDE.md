# SAML signed AuthnRequest and encrypted assertions

**Version:** v0.2.21-beta  
**Scope:** SP signing for HTTP-Redirect `AuthnRequest` and optional encrypted assertion decryption.

Complements [20-SAML-SP-BINDING-GUIDE.md](20-SAML-SP-BINDING-GUIDE.md) (base SP-initiated flow) and [16-SAML-SSO-STUB.md](16-SAML-SSO-STUB.md) (dev stub).

---

## When to enable

| Requirement | Env |
|-------------|-----|
| IdP requires signed AuthnRequests | Set `SAML_SP_PRIVATE_KEY` + `SAML_SP_CERTIFICATE` (auto-enables signing) |
| IdP encrypts assertions to SP | Set `SAML_WANT_ENCRYPTED_ASSERTIONS=true` + SP key/cert (required for decryption) |

Unsigned AuthnRequests remain supported when SP key/cert are omitted (same as v0.2.14+ SP binding).

---

## Environment variables

```bash
SSO_ENABLED=true
SSO_PROVIDER=saml
SAML_STUB_MODE=false
SSO_APP_BASE_URL=https://staging.example.com

SAML_METADATA_URL=https://idp.example.com/app/metadata
SAML_ENTITY_ID=https://staging.example.com/saml/metadata
SAML_ACS_URL=https://staging.example.com/api/v1/auth/sso/saml/acs

# SP signing + assertion decryption (PEM; multiline OK in .env)
SAML_SP_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----
...
-----END PRIVATE KEY-----"
SAML_SP_CERTIFICATE="-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----"

# Optional — decrypt encrypted assertions when IdP publishes encryption cert in metadata
SAML_WANT_ENCRYPTED_ASSERTIONS=true
```

| Variable | Required | Purpose |
|----------|----------|---------|
| `SAML_SP_PRIVATE_KEY` | When signing or encryption wanted | SP private key (RSA) |
| `SAML_SP_CERTIFICATE` | When signing or encryption wanted | SP public cert registered at IdP |
| `SAML_WANT_ENCRYPTED_ASSERTIONS` | — | `true` to require encrypted assertions (needs SP key/cert) |

`./scripts/validate-staging-env.sh` enforces SP key+cert when either SP env is set or `SAML_WANT_ENCRYPTED_ASSERTIONS=true`.

---

## IdP checklist

1. Register **SP certificate** from `GET /api/v1/auth/sso/saml/metadata` (or your `SAML_SP_CERTIFICATE`).
2. Enable **signed AuthnRequests** if your IdP policy requires it.
3. For encrypted assertions: configure IdP to encrypt to SP cert; ensure IdP metadata includes `KeyDescriptor use="encryption"`.
4. Smoke: SSO start URL must include `Signature=` and `SigAlg=` when SP signing is configured.

---

## Generate test keys (local only)

```bash
openssl req -new -x509 -days 365 -nodes \
  -subj "/CN=sp.example.com" \
  -keyout sp-test-private.pem -out sp-test-certificate.pem
```

Never commit production keys. CI uses `SAML_STUB_MODE=true` (no real IdP).

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.21-beta
./scripts/validate-staging-env.sh
./scripts/staging-dogfood.sh   # with SAML_STUB_MODE=false + SP keys on staging host
```

Provider probes ([24-STAGING-PROVIDER-PROBES-GUIDE.md](24-STAGING-PROVIDER-PROBES-GUIDE.md)) still validate metadata and SSO start; signed redirect URLs are exercised when SP keys are configured.
