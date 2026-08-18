# 09 — Testing Strategy

## 1. Backend — pytest

```bash
pip install pytest
```

Cover:

- Authentication
- Trainee CRUD
- Validation
- Database operations
- Authorization (roles)

## 2. API — Postman + Swagger

- Maintain a Postman collection per milestone
- Use FastAPI `/docs` during development
- Assert status codes and error message shapes

## 3. Frontend

Manual / later automated checks:

- Login
- Navigation
- Forms + validation
- API error handling
- Responsive layout

## 4. When to introduce each layer

| Layer | When |
|---|---|
| Swagger smoke | From Sprint 3 |
| Postman CRUD suite | Sprint 7 |
| pytest service/API tests | Sprint 7–8 |
| Frontend checklist | Sprint 9–10 |
| CI running tests | Phase 13 (after local stability) |

## 5. Quality bar for PRs

A feature is not ready for mentor review without:

- Happy-path verification
- At least one validation / error-path check
- Notes in the PR describing what was tested
