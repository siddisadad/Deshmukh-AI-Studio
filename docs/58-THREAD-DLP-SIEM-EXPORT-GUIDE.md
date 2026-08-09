# Thread DLP connectors and SIEM export

**Version:** v0.2.53-beta  
**Scope:** Per-organization DLP webhook/SIEM connectors, richer built-in patterns, DLP event audit log, and scheduled SIEM export.

Complements global export DLP ([44-THREAD-EXPORT-WATERMARK-DLP-GUIDE.md](44-THREAD-EXPORT-WATERMARK-DLP-GUIDE.md)).

---

## Database (V30)

### `organization_dlp_connectors`

| Column | Purpose |
|--------|---------|
| `slug` | Unique per org |
| `connector_type` | `WEBHOOK` (notify on match) or `SIEM` (batch export target) |
| `display_name` | Settings UI label |
| `webhook_url` | POST target for webhook or SIEM delivery |
| `enabled` | Active connector |
| `block_on_match` | Block exports when WEBHOOK connector matches (org-level) |
| `custom_patterns_json` | Optional JSON array of `{category, pattern, description}` |

### `thread_export_dlp_events`

Audit log for DLP matches on thread exports:

| Column | Purpose |
|--------|---------|
| `match_categories` | Comma-separated category list |
| `blocked` | Whether export was blocked |
| `siem_exported_at` | Set after successful SIEM delivery |

---

## Built-in patterns

In addition to SSN, PEM private keys, internal hostnames, and AWS secret keys:

| Category | Example |
|----------|---------|
| `credit_card` | 13–19 digit card numbers |
| `github_pat` | `ghp_…` tokens |
| `google_api_key` | `AIza…` keys |
| `slack_token` | `xoxb-…` / `xoxp-…` tokens |

Org connectors may add regex patterns via `custom_patterns_json`.

---

## API

### Organization settings (OWNER for create/delete; members can list)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/organizations/{orgId}/dlp/connectors` | List connectors |
| `POST` | `/organizations/{orgId}/dlp/connectors` | Create connector |
| `DELETE` | `/organizations/{orgId}/dlp/connectors/{connectorId}` | Remove connector |
| `GET` | `/organizations/{orgId}/dlp/events` | List DLP events |

### Operator SIEM export

| Method | Path | Auth |
|--------|------|------|
| `POST` | `/api/v1/exports/siem/run` | `BILLING_USAGE_SYNC_TOKEN` |

Exports pending `thread_export_dlp_events` (where `siem_exported_at` is null) to enabled org `SIEM` connectors.

Payload event type: `chat_export_dlp_siem`.

---

## Policy behavior

`ThreadExportDlpPolicyService` runs when global DLP is enabled **or** the org has an enabled `WEBHOOK` connector:

1. Scan export body with built-in + org custom patterns
2. Record `thread_export_dlp_events` row
3. Notify global webhook (if configured) and org `WEBHOOK` connectors
4. Block if global `CHAT_EXPORT_DLP_BLOCK_ON_MATCH=true` or any enabled org WEBHOOK has `block_on_match`

SIEM connectors do not trigger scans; they receive batched events.

---

## Scheduler and cron

| Env var | Default | Purpose |
|---------|---------|---------|
| `EXPORT_SIEM_EXPORT_ENABLED` | `false` | Enable `SiemExportScheduler` |
| `EXPORT_SIEM_EXPORT_INTERVAL_MS` | `300000` | Scheduler interval (5m) |

Cron script:

```bash
export BILLING_USAGE_SYNC_TOKEN=...
./scripts/scheduled-siem-export.sh
```

---

## UI

`/settings/dlp` — list connectors, add WEBHOOK/SIEM connectors, view recent DLP events.

---

## Staging smoke

```bash
export IMAGE_TAG=v0.2.53-beta
# Create SIEM connector in /settings/dlp, trigger export with DLP match, then:
curl -s -X POST "$API_URL/api/v1/exports/siem/run" \
  -H "Authorization: Bearer $BILLING_USAGE_SYNC_TOKEN"
```
