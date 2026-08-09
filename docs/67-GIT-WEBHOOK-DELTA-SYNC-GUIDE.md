# Git webhook incremental delta sync

**Version:** v0.2.62-beta  
**Scope:** Parse push webhook payloads and sync only changed/removed code paths instead of full repository tree replace.

Complements [66-GIT-SYNC-CONTENT-HYDRATION-GUIDE.md](66-GIT-SYNC-CONTENT-HYDRATION-GUIDE.md).

---

## Overview

On **push webhooks**, the API extracts `added`, `modified`, and `removed` paths from GitHub, GitLab, and Bitbucket payloads. When `GIT_SYNC_WEBHOOK_DELTA=true` (default), `CODE_METADATA_SYNC` jobs fetch only changed paths, apply upserts/deletes, and reindex — avoiding full tree scans.

Manual **Sync now** and webhooks without parseable deltas still perform full sync.

---

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `GIT_SYNC_WEBHOOK_DELTA` | `true` | Enable incremental webhook sync |

---

## Job payload

Webhook-enqueued jobs include:

```json
{
  "source": "webhook",
  "changedPaths": ["src/App.java", "README.md"],
  "removedPaths": ["legacy/Old.java"]
}
```

Only code-like extensions are indexed; branch `ref` must match the project git link branch.

---

## Providers

`GitMetadataPort.fetchFilesByPaths` fetches individual paths per host, then optional content hydration runs.

---

## Tests

- `GitWebhookPayloadParserTest` — GitHub / Bitbucket payload parsing
- `ProjectGitSyncDeltaIT` — delta apply removes README and adds changed path
