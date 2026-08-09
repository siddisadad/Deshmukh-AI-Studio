# Git code metadata sync and webhooks

**Version:** v0.2.59-beta  
**Scope:** Per-project GitHub link, manual/background sync, and push webhook → code metadata RAG reindex.

Complements [63-CODE-METADATA-RAG-GUIDE.md](63-CODE-METADATA-RAG-GUIDE.md).

---

## Overview

Projects can link a GitHub repository (`owner/name`) and branch. The API fetches the repository tree (mock provider in CI/dev, GitHub API in production), replaces the code metadata manifest, and reindexes `CODE_FILE` knowledge chunks.

GitHub **push** webhooks enqueue a `CODE_METADATA_SYNC` background job (signature verified per project).

---

## Database

V35 migration — `project_git_links` (one link per project):

| Column | Purpose |
|--------|---------|
| `repository` | `owner/name` |
| `branch` | Branch to sync (default `main`) |
| `webhook_secret` | HMAC secret for GitHub `X-Hub-Signature-256` |
| `last_synced_at` / `last_sync_status` / `last_sync_error` | Sync audit |

---

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `GIT_METADATA_PROVIDER` | `mock` | `mock` (CI/dev) or `github` |
| `GITHUB_SYNC_TOKEN` | — | PAT for GitHub API when `provider=github` |
| `GITHUB_API_BASE_URL` | `https://api.github.com` | GitHub API base |
| `API_PUBLIC_BASE_URL` | `http://localhost:8080` | Webhook URL host in API responses |

---

## API

JWT project endpoints:

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/v1/projects/{id}/git-link` | Link status + webhook URL/secret |
| `PUT` | `/api/v1/projects/{id}/git-link` | Upsert repository, branch, enabled |
| `POST` | `/api/v1/projects/{id}/git-link/sync` | Sync now (inline) |
| `POST` | `/api/v1/projects/{id}/git-link/sync/async` | Enqueue `CODE_METADATA_SYNC` job |

Public webhook (no JWT):

| Method | Path | Notes |
|--------|------|-------|
| `POST` | `/api/v1/git/webhook/github/{projectId}` | GitHub push webhook; verifies `X-Hub-Signature-256` |

Configure GitHub webhook: **content type** `application/json`, secret = `webhookSecret` from `GET git-link`.

---

## Background jobs

New job type: `CODE_METADATA_SYNC` — runs Git tree fetch, manifest replace, and code-file reindex.

---

## UI

Project settings → **Git repository sync** — repository, branch, webhook hints, sync now / background.

---

## Staging deploy

```bash
export IMAGE_TAG=v0.2.59-beta
export GIT_METADATA_PROVIDER=github
export GITHUB_SYNC_TOKEN=...
export API_PUBLIC_BASE_URL=https://api.staging.example.com
```

---

## Related

| Doc | Topic |
|-----|-------|
| [63-CODE-METADATA-RAG-GUIDE.md](63-CODE-METADATA-RAG-GUIDE.md) | Manual manifest upload |
| [18-JOB-WORKER-AUTOSCALING-GUIDE.md](18-JOB-WORKER-AUTOSCALING-GUIDE.md) | Background worker |
