# Scheduled chat archive sync

Off-site retention for **conversation thread exports** (complements in-app single/bulk export and Loki log archives in [17-LOG-ARCHIVE-GUIDE.md](17-LOG-ARCHIVE-GUIDE.md)).

**MVP scope:** cron-friendly shell script that calls `GET /projects/{id}/conversations/export` for every org project, writes **gzipped** archives locally, and optionally uploads to S3.

---

## 1. What gets archived

Per project, the script downloads the same bulk export the chat UI **Export all** button uses:

- **JSON** (default): structured archive with `conversationCount`, messages, roles, timestamps
- **Markdown**: human-readable `# Project archive` document

Private threads are included only when the archive account can see them (creator or org admin). Use a dedicated **archive service account** with org membership.

---

## 2. Run manually

```bash
# Long-lived JWT from a service user (recommended for cron)
export CHAT_ARCHIVE_ACCESS_TOKEN="eyJ..."
export CHAT_ARCHIVE_ORG_ID="00000000-0000-0000-0000-000000000001"

./scripts/scheduled-chat-archive.sh
```

Or login with email/password (org id inferred from login or `/me`):

```bash
export CHAT_ARCHIVE_EMAIL=archiver@company.com
export CHAT_ARCHIVE_PASSWORD='...'
export CHAT_ARCHIVE_FORMAT=markdown
export CHAT_ARCHIVE_DIR=./backups/chat
./scripts/scheduled-chat-archive.sh
```

Against staging edge (nginx `/api/v1` proxy):

```bash
export EDGE_URL=https://staging.example.com
export CHAT_ARCHIVE_ACCESS_TOKEN=...
./scripts/scheduled-chat-archive.sh
```

Direct API (no nginx):

```bash
export API_URL=http://localhost:8080
./scripts/scheduled-chat-archive.sh
```

### Environment variables

| Variable | Default | Meaning |
|---|---|---|
| `CHAT_ARCHIVE_ACCESS_TOKEN` | — | Bearer JWT (or set email/password) |
| `CHAT_ARCHIVE_EMAIL` / `CHAT_ARCHIVE_PASSWORD` | — | Login fallback for cron without static JWT |
| `CHAT_ARCHIVE_ORG_ID` | first org on `/me` | Organization to archive |
| `CHAT_ARCHIVE_FORMAT` | `json` | `json` or `markdown` |
| `CHAT_ARCHIVE_DIR` | `./backups/chat` | Base output directory |
| `CHAT_ARCHIVE_PROJECT_STATUS` | `ACTIVE` | Project list filter (`ACTIVE`, `ARCHIVED`, …) |
| `CHAT_ARCHIVE_ASSISTANT_ROLE` | — | Optional filter (`DEVELOPER`, `BA`, …) |
| `CHAT_ARCHIVE_S3_URI` | — | Optional `s3://bucket/prefix` upload (requires AWS CLI) |
| `EDGE_URL` | `http://localhost:8088` | Nginx edge when `API_URL` unset |
| `API_URL` | — | Direct API base (e.g. `http://localhost:8080`) |

Output layout:

```
backups/chat/20260808-021500/
  BE-threads.json.gz
  OPS-threads.json.gz
  manifest.json
  manifest.json.gz
```

`manifest.json` lists `projectId`, filename, and gzipped size for audit.

---

## 3. Cron + S3 (daily off-site sync)

```bash
export CHAT_ARCHIVE_S3_URI=s3://my-company-aistudio-archives/chat
export CHAT_ARCHIVE_ACCESS_TOKEN=...
export CHAT_ARCHIVE_ORG_ID=...
export EDGE_URL=https://staging.example.com
./scripts/scheduled-chat-archive.sh
```

**Cron example** (daily 03:00 UTC):

```cron
0 3 * * * cd /opt/aistudio && set -a && source /opt/aistudio/.env.archive && set +a && ./scripts/scheduled-chat-archive.sh
```

Store secrets in `/opt/aistudio/.env.archive` (not committed). Rotate the service account password or refresh JWT on your IdP schedule.

### Bucket policy

- Restrict to archive IAM user/role (write on prefix only).
- Enable versioning or lifecycle transition to Glacier for compliance retention.
- Do not expose the bucket publicly.

Pair with Loki export cron ([17-LOG-ARCHIVE-GUIDE.md](17-LOG-ARCHIVE-GUIDE.md)) for full observability + conversation retention.

---

## 4. Service account setup

1. Register or provision `archiver@company.com` (or SSO/OIDC user).
2. Add to the target organization with **MEMBER** (or **ADMIN** if archiving all private threads across users).
3. Issue a long-lived token via login in staging, or automate login in cron with password from secret store.
4. Verify: create a test thread, run script, confirm gzip contains messages.

---

## 5. Restore / audit

```bash
gzip -dc backups/chat/20260808-021500/BE-threads.json.gz | jq '.conversationCount'
gzip -dc backups/chat/20260808-021500/manifest.json.gz | jq .
```

Markdown archives open directly after `gzip -dc`. JSON archives match the API export schema used by integration tests in `AssistantControllerIT`.

---

## 6. Related

| Doc | Topic |
|---|---|
| [17-LOG-ARCHIVE-GUIDE.md](17-LOG-ARCHIVE-GUIDE.md) | Loki NDJSON export + S3 |
| [18-JOB-WORKER-AUTOSCALING-GUIDE.md](18-JOB-WORKER-AUTOSCALING-GUIDE.md) | Background job workers |
| [04-API-SPECIFICATION.md](04-API-SPECIFICATION.md) | REST contracts |
