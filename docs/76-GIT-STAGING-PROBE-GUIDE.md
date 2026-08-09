# Git Staging Probe Guide

**Version:** v0.2.71-beta

Git host readiness checks integrated into `scripts/staging-provider-probes.sh` (see also [24-STAGING-PROVIDER-PROBES-GUIDE.md](24-STAGING-PROVIDER-PROBES-GUIDE.md)).

## 1. When probes run

Skipped when `GIT_METADATA_PROVIDER=mock` (CI and local mock stacks).

When not mock, probes run after billing/SSO/mail checks in `staging-dogfood.sh` step 5.

## 2. Direct API token probes

| Env var | Probe |
|---|---|
| `GITHUB_SYNC_TOKEN` | `GET {GITHUB_API_BASE_URL}/user` |
| `GITLAB_SYNC_TOKEN` | `GET {GITLAB_API_BASE_URL}/user` |
| `BITBUCKET_SYNC_TOKEN` | `GET {BITBUCKET_API_BASE_URL}/user` |

Unset tokens are skipped (not a failure).

## 3. Application API probe

When at least one platform token is set, the script calls:

`POST /api/v1/organizations/{orgId}/git-credentials/{provider}/test`

using the probe user's org and the first provider with a configured token (github → gitlab → bitbucket).

Validates credential resolution and `GitConnectionProbeService` on the running API.

## 4. Credential rotation audit

Org credential changes are recorded in `organization_git_credential_events`:

| Action | When |
|---|---|
| `CREATED` | First upsert for provider |
| `TOKEN_ROTATED` | Upsert with new `apiToken` |
| `UPDATED` | Upsert without token change |
| `DELETED` | Org credential removed |

API: `GET /api/v1/organizations/{orgId}/git-credentials/events?limit=50`

Settings → Git shows the rotation audit trail.

## 5. Tests

- `OrgGitCredentialControllerIT.ownerCanUpsertAndTestGitCredential` — events list after create/delete
