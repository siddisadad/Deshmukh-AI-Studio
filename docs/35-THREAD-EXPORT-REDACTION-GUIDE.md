# Thread export redaction policies

**Version:** v0.2.30-beta  
**Scope:** Named redaction policies for single-thread, bulk, and compliance purge exports.

Complements bulk archive sync ([19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md)) and compliance purge ([30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md](30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md)).

---

## Policies

| Policy | Redacts |
|--------|---------|
| `none` | Nothing (default for dev/CI) |
| `pii` | Emails, phone numbers, credit-card patterns |
| `secrets` | Bearer tokens, OpenAI/Anthropic keys, AWS access keys, JSON `password`/`secret`/`token` fields, common env secret lines |
| `standard` | `pii` + `secrets` |

Exports include `redactionPolicy` in JSON (and a Markdown header line) when policy is not `none`.

---

## Environment

```bash
# Default for user-initiated and scheduled bulk exports
CHAT_EXPORT_REDACTION_POLICY=standard

# Compliance purge gzip archives (default none — full fidelity for legal retention)
CHAT_EXPORT_COMPLIANCE_REDACTION_POLICY=none
```

Maps to `aistudio.ai.conversation.export-redaction-policy` and `compliance-export-redaction-policy` in `application.yml`.

---

## API

```http
GET /api/v1/conversations/{id}/export?format=json&redaction=standard
GET /api/v1/projects/{id}/conversations/export?format=markdown&redaction=pii
```

Optional `redaction` query param overrides the server default for that download. Invalid values return `400`.

Compliance purge (`POST .../retention-purge` with `complianceExport: true`) uses `CHAT_EXPORT_COMPLIANCE_REDACTION_POLICY` only (no query override).

---

## Scheduled archive cron

```bash
export CHAT_EXPORT_REDACTION_POLICY=standard
./scripts/scheduled-chat-archive.sh
```

The script forwards `redaction` to the bulk export API when the env var is set.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.30-beta
export CHAT_EXPORT_REDACTION_POLICY=standard

# After posting a message containing an email and API key pattern:
curl -fsS "$API/api/v1/conversations/$THREAD_ID/export?format=json&redaction=standard" \
  -H "Authorization: Bearer $TOKEN" | grep REDACTED
```

---

## Related

| Doc | Topic |
|-----|-------|
| [19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md) | Scheduled project archives |
| [30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md](30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md) | Purge compliance gzip |
| [25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md](25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md) | Retention + legal hold |
