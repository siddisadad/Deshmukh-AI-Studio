# 04 — API Specification (Planned)

Base path: `/api`  
Format: JSON  
Auth (from Sprint 8): `Authorization: Bearer <jwt>`

## 1. HTTP methods (Sprint 4)

| Method | Use |
|---|---|
| GET | Read |
| POST | Create |
| PUT | Full update |
| PATCH | Partial update (optional) |
| DELETE | Delete / deactivate |

## 2. Trainee APIs (Sprint 7 — first milestone)

| Method | Path | Description |
|---|---|---|
| GET | `/api/trainees` | List trainees |
| GET | `/api/trainees/{id}` | Get one trainee |
| POST | `/api/trainees` | Create trainee |
| PUT | `/api/trainees/{id}` | Update trainee |
| DELETE | `/api/trainees/{id}` | Delete or deactivate |

### Validation rules

| Field | Rule |
|---|---|
| Name | Required |
| Email | Valid email |
| Mobile | Valid format |
| Batch | Required (or valid `batch_id`) |
| Joining date | Valid date |
| Status | One of `ACTIVE`, `INACTIVE`, `COMPLETED`, `DROPPED` |

## 3. Authentication (Sprint 8)

Flow:

```
Login
 ↓
Validate user
 ↓
Verify password
 ↓
Generate JWT
 ↓
Return token
 ↓
Access protected APIs
```

Protected example:

```
GET /api/trainees
Authorization: Bearer <token>
```

## 4. Batch APIs (Sprint 10)

| Method | Path |
|---|---|
| POST | `/api/batches` |
| GET | `/api/batches` |
| GET | `/api/batches/{id}` |
| PUT | `/api/batches/{id}` |
| DELETE | `/api/batches/{id}` |

## 5. Attendance (Sprint 11)

| Method | Path | Notes |
|---|---|---|
| POST | `/api/attendance` | Mark attendance |
| GET | `/api/attendance` | List / filter |

Reports (later): daily, monthly, trainee attendance %.

## 6. Tasks (Sprint 12)

Mentor creates tasks; trainee submits; mentor reviews and completes.

Exact resource paths to be finalized in the sprint issue (e.g. `/api/tasks`, `/api/submissions`).

## 7. Error responses

APIs should return meaningful HTTP status codes:

| Code | Meaning |
|---|---|
| 200 | OK |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict (e.g. duplicate email) |
| 500 | Internal Server Error |

Example body:

```json
{
  "message": "Trainee not found"
}
```

## 8. Tooling for API learning

- FastAPI Swagger: `/docs`
- Postman collections for each sprint milestone
