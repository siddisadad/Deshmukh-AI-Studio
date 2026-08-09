# Plugin marketplace and third-party packs

**Version:** v0.2.55-beta  
**Scope:** Curated marketplace plugin packs, per-org install/uninstall, and third-party tool SPI extensions.

Complements the plugin SPI and org toggles on `/settings/plugins`.

---

## Marketplace packs

Packs are defined in `backend/src/main/resources/plugin-packs/*.json` and synced to `plugin_packs` on startup.

| Pack | Plugins |
|------|---------|
| `pack.thirdparty.devtools` | Markdown preview, word count |
| `pack.thirdparty.compliance` | Redaction scan, export checklist |

Third-party plugins ship in the application binary (SPI) but are **hidden** until the org installs the pack.

---

## Database (V32)

- `plugin_packs` — marketplace catalog metadata
- `plugin_pack_members` — pack → plugin membership
- `organization_plugin_packs` — per-org installs

---

## API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/plugins/marketplace` | List marketplace packs |
| `GET` | `/api/v1/organizations/{orgId}/plugin-packs` | Packs + install status |
| `POST` | `/api/v1/organizations/{orgId}/plugin-packs/{packId}/install` | Install (OWNER) |
| `DELETE` | `/api/v1/organizations/{orgId}/plugin-packs/{packId}` | Uninstall (OWNER) |

Existing org plugin endpoints list only visible plugins (built-in + sample echo + installed pack tools).

---

## UI

`/settings/plugins` — **Marketplace packs** section with Install/Uninstall; tools appear under Tools after install.

---

## Invoke tools

After install, invoke pack tools via:

`POST /api/v1/projects/{projectId}/tools/{toolId}/invoke`

Uninstall removes org plugin rows and blocks invocation.

---

## Adding a new pack

1. Implement `ToolPlugin` beans under `infrastructure/plugin/thirdparty/`
2. Add `plugin-packs/your-pack.json` manifest listing `pluginIds`
3. Restart API — catalog sync registers pack + members

---

## Related

| Doc | Topic |
|-----|-------|
| [14-STAGING-DOGFOOD-GUIDE.md](14-STAGING-DOGFOOD-GUIDE.md) | Staging plugin checklist |
| [12-TESTING-STRATEGY.md](12-TESTING-STRATEGY.md) | Plugin E2E smoke |
