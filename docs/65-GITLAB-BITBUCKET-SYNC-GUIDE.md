# GitLab and Bitbucket code metadata sync

**Version:** v0.2.60-beta  
**Scope:** Per-project GitLab / Bitbucket connectors, webhook verification, and `CODE_METADATA_SYNC` jobs.

Complements [64-GIT-CODE-METADATA-SYNC-GUIDE.md](64-GIT-CODE-METADATA-SYNC-GUIDE.md).

---

## Overview

Projects can link repositories on **GitHub**, **GitLab**, or **Bitbucket**. Each link stores a `provider`, `repository` (`owner/name`, `namespace/project`, or `workspace/slug`), branch, and webhook secret.

The API routes sync to the correct host connector via `GitMetadataRegistry`. In mock mode (`GIT_METADATA_PROVIDER=mock`), all three providers use the mock tree fetcher for CI and local dev.

---

## Database

V37 migration — extends `project_git_links.provider` CHECK to include `gitlab` and `bitbucket`.

---

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `GITLAB_SYNC_TOKEN` | — | GitLab PAT / project token when syncing GitLab repos |
| `GITLAB_API_BASE_URL` | `https://gitlab.com/api/v4` | GitLab API base (self-managed override) |
| `BITBUCKET_SYNC_TOKEN` | — | Bitbucket OAuth / app password / workspace token |
| `BITBUCKET_API_BASE_URL` | `https://api.bitbucket.org/2.0` | Bitbucket API base |

GitHub settings from [64-GIT-CODE-METADATA-SYNC-GUIDE.md](64-GIT-CODE-METADATA-SYNC-GUIDE.md) still apply when `provider=github`.

---

## API

`PUT /api/v1/projects/{id}/git-link` accepts optional `provider`: `github`, `gitlab`, or `bitbucket` (default `github`).

Webhook URLs (no JWT):

| Provider | Path | Verification |
|----------|------|--------------|
| GitHub | `POST /api/v1/git/webhook/github/{projectId}` | `X-Hub-Signature-256` HMAC |
| GitLab | `POST /api/v1/git/webhook/gitlab/{projectId}` | `X-Gitlab-Token` shared secret |
| Bitbucket | `POST /api/v1/git/webhook/bitbucket/{projectId}` | `X-Hub-Signature-256` HMAC |

---

## Host setup

### GitLab

1. Create project webhook → URL from `GET git-link` (`webhookUrl`).
2. Secret token = `webhookSecret` from `GET git-link`.
3. Enable **Push events**.
4. Set `GITLAB_SYNC_TOKEN` with `read_api` (or repo read) scope.

### Bitbucket

1. Repository webhook → URL from `GET git-link`.
2. Secret = `webhookSecret`.
3. Enable repository push events.
4. Set `BITBUCKET_SYNC_TOKEN` with repository read scope.

---

## UI

Project settings → **Git repository sync** — provider selector (GitHub / GitLab / Bitbucket), repository, branch, webhook hints, sync now / background.

---

## Staging deploy

```bash
export IMAGE_TAG=v0.2.60-beta
export GITLAB_SYNC_TOKEN=...
export BITBUCKET_SYNC_TOKEN=...
# optional self-managed GitLab
export GITLAB_API_BASE_URL=https://gitlab.example.com/api/v4
```

---

## Tests

- `GitWebhookControllerIT` — GitHub, GitLab (`X-Gitlab-Token`), and Bitbucket webhook enqueue paths
- `ProjectGitLinkControllerIT` — GitLab provider upsert + mock sync
