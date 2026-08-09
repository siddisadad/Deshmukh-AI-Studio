# Git Webhook Secret UX Guide

**Version:** v0.2.72-beta

Project settings improvements for git webhook configuration: regenerate secret, copy URL/secret, and disconnect link.

## 1. API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/v1/projects/{id}/git-link/regenerate-webhook-secret` | New HMAC secret; update git host webhook |
| `DELETE` | `/api/v1/projects/{id}/git-link` | Remove link (`204`); `GET` returns empty shell |

Existing `PUT` with `regenerateWebhookSecret: true` still works; the dedicated POST avoids resubmitting repository fields.

## 2. Webhook URL

Returned on `GET/PUT git-link` as `webhookUrl`:

`{API_PUBLIC_BASE_URL}/api/v1/git/webhook/{provider}/{projectId}`

Providers: `github`, `gitlab`, `bitbucket`.

## 3. UI (project settings)

- Provider-specific webhook setup hint
- Copy webhook URL and secret
- Reveal/hide secret
- **Regenerate secret** — calls regenerate endpoint
- **Disconnect** — deletes link and clears form state

## 4. Host configuration

| Provider | Secret field | Events |
|---|---|---|
| GitHub | Webhook secret | Push (and optional PR) |
| GitLab | Secret token | Push events |
| Bitbucket | Secret | Repo push |

After regenerating, update the secret on the git host — old webhooks will fail signature verification.

## 5. Tests

- `ProjectGitLinkControllerIT.regenerateWebhookSecretAndDisconnectLink`
