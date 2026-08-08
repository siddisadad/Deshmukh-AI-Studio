# Chat retention and legal hold guide

**Version:** v0.2.20-beta  
**Scope:** Per-project thread retention policy, per-thread legal hold, and automated purge.

---

## Concepts

| Feature | Behavior |
|---------|----------|
| **chatRetentionDays** (project) | Threads expire N days after `updated_at` (last activity) |
| **retentionExpiresAt** (thread) | Computed from project policy; shown in UI |
| **legalHold** (thread) | Blocks manual delete and automated purge |
| **retention-purge** | Deletes expired threads that are not on legal hold |

Retention is anchored to **last update** (new messages refresh expiry).

---

## API

| Method | Path | Notes |
|--------|------|-------|
| `PATCH` | `/projects/{id}` | `chatRetentionDays` (1–3650) or `clearChatRetention: true` |
| `PATCH` | `/conversations/{id}` | `legalHold: true/false` |
| `POST` | `/projects/{id}/conversations/retention-purge` | Returns `{ "purgedCount": N }` |

Delete thread returns `LEGAL_HOLD` when hold is active.

---

## UI

- **Project settings:** retention days field + “Purge expired threads now”
- **AI Chat:** gavel icon toggles legal hold; delete disabled when held; expiry shown in thread list

---

## Cron purge

Export archives first if required ([19-CHAT-ARCHIVE-SYNC-GUIDE.md](19-CHAT-ARCHIVE-SYNC-GUIDE.md)):

```bash
export CHAT_RETENTION_EMAIL=ops@company.com
export CHAT_RETENTION_PASSWORD=...
./scripts/scheduled-chat-retention.sh
```

Daily example:

```cron
15 3 * * * cd /opt/aistudio && ./scripts/scheduled-chat-retention.sh >> /var/log/chat-retention.log 2>&1
```

---

## Staging smoke

```bash
export IMAGE_TAG=v0.2.20-beta
./scripts/staging-ghcr-deploy.sh
```

1. Project settings → set retention to **1** day → save
2. Chat → toggle legal hold on one thread
3. Settings → “Purge expired threads now” (only expired non-held threads removed)

---

**Previous:** [24-STAGING-PROVIDER-PROBES-GUIDE.md](24-STAGING-PROVIDER-PROBES-GUIDE.md) · **Next:** [09-DEVELOPMENT-ROADMAP.md](09-DEVELOPMENT-ROADMAP.md)
