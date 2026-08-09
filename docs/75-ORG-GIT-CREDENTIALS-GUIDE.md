# Organization Git Credentials Guide

**Version:** v0.2.70-beta

Per-organization Git host API tokens (PATs) for private repository sync. Org credentials override platform environment tokens when enabled.

## 1. Credential resolution order

For each sync or connection test:

1. **Org credential** — enabled row in `organization_git_credentials` for the project's organization and link provider
2. **Platform env** — `GITHUB_SYNC_TOKEN`, `GITLAB_SYNC_TOKEN`, `BITBUCKET_SYNC_TOKEN` (and optional base URLs)
3. **Error** — `CONFIG_ERROR` when neither is available

Mock mode (`aistudio.git.provider=mock`) uses the mock connector for all providers in CI and local dev.

## 2. Schema

Table `organization_git_credentials` (migration `V43`):

| Column | Notes |
|---|---|
| `provider` | `github`, `gitlab`, or `bitbucket` (one row per org per provider) |
| `display_name` | Owner-facing label |
| `api_token` | PAT; never returned by list API |
| `api_base_url` | Optional — self-managed GitLab or custom API host |
| `enabled` | When false, falls back to platform env |
| `last_tested_at`, `last_test_status`, `last_test_error` | Updated by org test endpoint |

## 3. API (OWNER for write/test)

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/v1/organizations/{orgId}/git-credentials` | Lists all three providers with `configured` and `credentialSource` |
| `PUT` | `/api/v1/organizations/{orgId}/git-credentials/{provider}` | Upsert; `apiToken` required on first create |
| `DELETE` | `/api/v1/organizations/{orgId}/git-credentials/{provider}` | Remove org override |
| `POST` | `/api/v1/organizations/{orgId}/git-credentials/{provider}/test` | Optional `repository` and `branch` query params |

Project-level test (uses linked repo):

| Method | Path |
|---|---|
| `POST` | `/api/v1/projects/{id}/git-link/test` |

Response: `{ ok, message, checks: [{ name, status, message }] }`

## 4. UI

- **Settings → Git** — OWNER upsert, test, and delete org credentials
- **Project settings → Git repository sync** — **Test connection** button before sync

## 5. Staging

Platform env tokens still work for dogfood when org credentials are not set. Set org PATs in staging to validate multi-tenant private-repo paths.

## 6. Tests

- `OrgGitCredentialControllerIT` — list, upsert, test, delete
- `ProjectGitLinkControllerIT.testConnectionReturnsOkForMockLink`
