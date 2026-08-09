# Thread export watermarking and DLP

**Version:** v0.2.39-beta  
**Scope:** Export provenance watermarks and DLP scanning for single-thread and bulk exports.

Complements export redaction ([35-THREAD-EXPORT-REDACTION-GUIDE.md](35-THREAD-EXPORT-REDACTION-GUIDE.md)) and scheduled archive sync ([19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md)).

---

## Watermarking

When `CHAT_EXPORT_WATERMARK_ENABLED=true`, exports embed provenance metadata:

| Field | JSON | Markdown |
|-------|------|----------|
| `exportId` | UUID for the download | Header + footer |
| `exportedByUserId` | Requesting user UUID | Header + footer |
| `exportedAt` | ISO timestamp (existing) | Header |
| `watermarkNotice` | Configurable notice text | Header + footer |

Compliance purge gzip archives are **not** watermarked (legal retention fidelity).

---

## DLP scanning

When `CHAT_EXPORT_DLP_ENABLED=true`, the API scans the final export body for high-risk patterns beyond redaction:

| Category | Example |
|----------|---------|
| `ssn` | US Social Security number (`123-45-6789`) |
| `private_key` | PEM `BEGIN PRIVATE KEY` blocks |
| `internal_hostname` | `*.internal`, `*.corp`, `*.local` |
| `aws_secret_key` | `AWS_SECRET_ACCESS_KEY` with 40-char key material |

On match:

1. Optional webhook notification (`CHAT_EXPORT_DLP_WEBHOOK_URL`) with `event=chat_export_dlp_match`
2. If `CHAT_EXPORT_DLP_BLOCK_ON_MATCH=true` (default), returns **403** with `FORBIDDEN` and category list

Webhook delivery failures are logged but do not block exports when blocking is disabled.

---

## Environment

```bash
CHAT_EXPORT_WATERMARK_ENABLED=true
CHAT_EXPORT_WATERMARK_NOTICE=Confidential — do not redistribute.

CHAT_EXPORT_DLP_ENABLED=true
CHAT_EXPORT_DLP_BLOCK_ON_MATCH=true
CHAT_EXPORT_DLP_WEBHOOK_URL=https://hooks.example.com/dlp
```

Maps to `aistudio.ai.conversation.export-watermark-*` and `export-dlp-*` in `application.yml`.

Defaults: watermark and DLP **disabled** (dev/CI); DLP blocks on match when enabled.

---

## API

Same export endpoints — no new query params:

```http
GET /api/v1/conversations/{id}/export?format=json
GET /api/v1/projects/{id}/conversations/export?format=markdown
```

DLP block response:

```json
{
  "code": "FORBIDDEN",
  "message": "Export blocked by DLP policy: ssn"
}
```

---

## Webhook payload

```json
{
  "event": "chat_export_dlp_match",
  "exportId": "…",
  "exportedByUserId": "…",
  "projectId": "…",
  "conversationId": "…",
  "matches": [
    { "category": "ssn", "description": "US Social Security number pattern" }
  ]
}
```

`conversationId` is omitted for project bulk exports.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.39-beta
export CHAT_EXPORT_WATERMARK_ENABLED=true
export CHAT_EXPORT_DLP_ENABLED=true

# Clean thread — expect watermark fields:
curl -fsS "$API/api/v1/conversations/$THREAD_ID/export?format=json" \
  -H "Authorization: Bearer $TOKEN" | grep exportId

# Thread with SSN pattern — expect 403:
curl -fsS -w "%{http_code}" "$API/api/v1/conversations/$THREAD_ID/export" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Related

| Doc | Topic |
|-----|-------|
| [35-THREAD-EXPORT-REDACTION-GUIDE.md](35-THREAD-EXPORT-REDACTION-GUIDE.md) | Redaction policies |
| [19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md) | Scheduled bulk exports |
| [30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md](30-COMPLIANCE-EXPORT-ON-PURGE-GUIDE.md) | Compliance purge archives |
