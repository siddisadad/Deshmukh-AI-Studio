# AI Studio API

Spring Boot 3.3 / Java 21 backend for AI Studio MVP.

## Run

```bash
# Requires PostgreSQL with DB/user aistudio (password aistudio by default)
mvn spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Test

```bash
mvn test
```

## Auth (implemented)

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`
- `GET|PATCH /api/v1/me`
- `POST /api/v1/me/password`
