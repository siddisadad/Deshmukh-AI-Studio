# 13 — First Assignment: Trainee Management

## Feature

**Trainee Management** — Admin must be able to manage trainees.

This is the first real development milestone (Sprint 7 + UI integration).

## Fields

| Field |
|---|
| Trainee ID |
| Name |
| Email |
| Mobile |
| Batch |
| Joining Date |
| Status |

## APIs

| Method | Path |
|---|---|
| POST | `/api/trainees` |
| GET | `/api/trainees` |
| GET | `/api/trainees/{id}` |
| PUT | `/api/trainees/{id}` |
| DELETE | `/api/trainees/{id}` |

## Frontend

```
Trainee List
       ↓
Add Trainee
       ↓
Edit Trainee
       ↓
View Details
       ↓
Deactivate
```

## Acceptance criteria

- [ ] API works
- [ ] Database works
- [ ] Validation works
- [ ] Error handling works
- [ ] Postman tests pass
- [ ] React UI works
- [ ] Git branch created
- [ ] Commit(s) created with clear messages
- [ ] Pull request submitted
- [ ] Mentor review completed
- [ ] Feedback fixed and merged

## Suggested branch

```
feature/trainee-crud
```

## Suggested PR title

```
feat: implement trainee CRUD
```

## Out of scope for this assignment

- Attendance
- Tasks / submissions
- Performance scoring
- Docker / CI/CD
- Full mentor/trainee portals beyond what is needed to demo trainee CRUD
