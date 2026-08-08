# Compliance export on retention purge

**Version:** v0.2.25-beta  
**Scope:** Gzip JSON archive of expired threads **before** automated retention purge.

Complements retention policy ([25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md](25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md)) and bulk archive sync ([19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md)).

---

## API

```http
POST /api/v1/projects/{projectId}/conversations/retention-purge
Content-Type: application/json

{"complianceExport": true}
```

| `complianceExport` | Response |
|--------------------|----------|
| `false` / omitted | JSON `{ "purgedCount", "exportedCount" }` |
| `true` | Gzip JSON archive (`application/gzip`) with headers `X-Purged-Count`, `X-Exported-Count` |

Archive includes full message history plus compliance fields: `legalHold`, `retentionExpiresAt`, `purgeReason`, `purgedAt`.

Legal-hold threads are never included (same as purge eligibility).

---

## Cron with compliance export

```bash
export CHAT_RETENTION_COMPLIANCE_EXPORT=true
export CHAT_RETENTION_COMPLIANCE_DIR=./backups/compliance-purge
# optional: CHAT_RETENTION_COMPLIANCE_S3_URI=s3://bucket/compliance-purge
./scripts/scheduled-chat-retention.sh
```

Pair with chat archive cron when you need both full-project and purge-specific compliance copies.

---

## Smoke test

```bash
export IMAGE_TAG=v0.2.25-beta
# Set retention 1 day, expire a thread, then:
curl -X POST "$API/api/v1/projects/$PROJECT_ID/conversations/retention-purge" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"complianceExport":true}' \
  --output compliance-purge.json.gz
gunzip -c compliance-purge.json.gz | head
```

---

## Related

| Doc | Topic |
|-----|-------|
| [25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md](25-CHAT-RETENTION-LEGAL-HOLD-GUIDE.md) | Retention + legal hold |
| [19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md) | Scheduled full-project export |
