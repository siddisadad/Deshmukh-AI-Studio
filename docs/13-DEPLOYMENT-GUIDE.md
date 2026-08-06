# Deployment Guide
## AI Studio for Software Engineering — MVP

Production-shaped deployment using **Docker Compose + Nginx** on a single VM. Suitable for early SaaS / private beta.

---

## 1. Architecture (Deployed)

```
Internet
   │
   ▼
Nginx (TLS, SPA static, /api proxy)
   │
   ├─► frontend static files
   └─► api:8080 (Spring Boot)
            │
            ├─► postgres:5432
            └─► redis:6379 (optional)
```

External: LLM provider APIs (OpenAI/Anthropic).

---

## 2. Prerequisites

- Docker Engine + Docker Compose v2
- Domain name + DNS A/AAAA record
- TLS certificates (Let’s Encrypt recommended)
- Secrets: `JWT_SECRET`, `DB_PASSWORD`, provider API key

Minimum VM (beta): 2 vCPU, 4 GB RAM, 40 GB SSD.

---

## 3. Repository Layout (deploy artifacts)

```
/
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env.example
├── nginx/
│   ├── nginx.conf
│   └── conf.d/aistudio.conf
├── scripts/
│   ├── backup-db.sh
│   └── restore-db.sh
├── backend/Dockerfile
└── frontend/Dockerfile
```

---

## 4. Environment Variables

Copy `.env.example` → `.env` (never commit `.env`).

```bash
# Database
DB_NAME=aistudio
DB_USER=aistudio
DB_PASSWORD=change-me-strong
POSTGRES_PORT=5432

# API
JWT_SECRET=use-a-long-random-string-at-least-32-chars
CORS_ORIGINS=https://app.example.com
AI_PROVIDER=anthropic
ANTHROPIC_API_KEY=sk-ant-...
# or OPENAI_API_KEY=...

# Mail (optional)
MAIL_HOST=smtp.example.com
MAIL_USER=...
MAIL_PASSWORD=...
MAIL_FROM=noreply@example.com

# Public URLs
PUBLIC_APP_URL=https://app.example.com
API_PUBLIC_URL=https://app.example.com/api/v1
```

Generate secrets:
```bash
openssl rand -base64 48
```

---

## 5. Docker Images

### Backend Dockerfile (multi-stage)
1. `maven:3.9-eclipse-temurin-21` → `./mvnw -DskipTests package`
2. `eclipse-temurin:21-jre` → copy jar, run as non-root user

### Frontend Dockerfile
1. `node:20-alpine` → `npm ci && npm run build`
2. Copy `dist/` into Nginx image **or** shared volume consumed by edge Nginx

Healthcheck API: `GET /actuator/health`.

---

## 6. Compose Services (prod profile)

```yaml
services:
  postgres:
    image: postgres:16
    volumes: [pgdata:/var/lib/postgresql/data]
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    restart: unless-stopped

  api:
    build: ./backend
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${DB_NAME}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      AI_PROVIDER: ${AI_PROVIDER}
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      CORS_ORIGINS: ${CORS_ORIGINS}
    depends_on: [postgres]
    restart: unless-stopped

  nginx:
    image: nginx:1.27-alpine
    ports: ["80:80", "443:443"]
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - frontend_dist:/usr/share/nginx/html:ro
      - certbot_etc:/etc/letsencrypt:ro
    depends_on: [api]
    restart: unless-stopped
```

---

## 7. Nginx Sketch

```nginx
server {
  listen 443 ssl http2;
  server_name app.example.com;

  # ssl_certificate ...;
  # ssl_certificate_key ...;

  root /usr/share/nginx/html;
  index index.html;

  location /api/ {
    proxy_pass http://api:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Request-Id $request_id;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_read_timeout 120s;  # AI calls
  }

  location /actuator/health {
    proxy_pass http://api:8080/actuator/health;
  }

  location / {
    try_files $uri /index.html;
  }
}
```

Disable Swagger UI in prod (`springdoc.swagger-ui.enabled=false`).

---

## 8. Deploy Steps

1. Clone release tag on VM.  
2. Create `.env` from example; fill secrets.  
3. `docker compose -f docker-compose.yml -f docker-compose.prod.yml build`  
4. `docker compose ... up -d`  
5. Verify: `curl -f https://app.example.com/actuator/health`  
6. Run smoke: register user via UI.  
7. Confirm Flyway version in logs (`Successfully applied`).

### Zero-downtime (MVP lite)
- `docker compose up -d --no-deps --build api` then reload nginx.
- Prefer maintenance window for first betas.

---

## 9. Database Migrations

- Migrations run on API startup via Flyway.
- **Backup before deploy:**
```bash
docker compose exec -T postgres pg_dump -U aistudio aistudio > backup-$(date +%F).sql
```
- Forward-only migrations; test on staging first.

---

## 10. CI/CD (GitHub Actions)

**On PR:** build + unit/IT + frontend lint/build.  
**On main/tag:** build images, push to registry (GHCR), optional SSH deploy script.

Never store provider keys in workflow logs; use GitHub Secrets.

---

## 11. Observability (MVP)

- `docker compose logs -f api`
- JSON logs shipped later to Loki/ELK if needed
- Uptime check on `/actuator/health`
- Disk alerts for Postgres volume

---

## 12. Backup & Restore

**Backup:** nightly cron `pg_dump` to object storage.  
**Restore:**
```bash
cat backup.sql | docker compose exec -T postgres psql -U aistudio aistudio
```
Test restore on staging quarterly.

---

## 13. Security Hardening Checklist

- [x] TLS-ready Nginx config (enable certs for production)
- [x] Strong `JWT_SECRET` and DB password required in prod compose overlay
- [x] CORS limited via `CORS_ORIGINS`
- [x] Swagger disabled in `prod` Spring profile
- [x] Non-root API container user
- [ ] Firewall: only 80/443 public (operator responsibility)
- [x] Rate limiting enabled
- [ ] OS packages updated (operator responsibility)

---

## 14. Local Dev vs Prod

| Concern | Dev | Prod |
|---|---|---|
| AI | mock default | real provider |
| Mail | logging adapter | SMTP |
| TLS | optional | required |
| Compose | hot reload mounts optional | immutable images |

---

## 15. Rollback

1. `docker compose` point API to previous image tag.  
2. If migration is incompatible, restore DB backup taken pre-deploy (plan migrations to be backward compatible when possible).

---

## 16. Prototype Note

The existing FastAPI prototype is **not** the production deploy target. Keep it for demos until Spring/React MVP replaces it. Do not expose the prototype publicly without auth.

---

## 17. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Compose + Nginx MVP deploy guide |

**Previous:** `12-TESTING-STRATEGY.md` · **Index:** `docs/README.md`
