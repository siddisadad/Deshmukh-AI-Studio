# 06 — Git and Repository Strategy

## 1. Central repository

Name: **`dttp-trainee-management`**

Recommended structure:

```
dttp-trainee-management/
├── backend/
├── frontend/
├── docs/
├── database/
├── docker/
├── .gitignore
├── README.md
└── docker-compose.yml
```

## 2. Branch strategy

```
main
  │
  └── development
        │
        ├── feature/authentication
        ├── feature/trainee-crud
        ├── feature/batch-management
        ├── feature/attendance
        └── feature/task-management
```

### Rules

- Trainees **must not** push directly to `main`
- Long-lived integration branch: `development`
- One feature branch per issue / assignment

## 3. Workflow

```
Issue
 ↓
Feature Branch
 ↓
Development (local)
 ↓
Testing
 ↓
Commit
 ↓
Push
 ↓
Pull Request
 ↓
Mentor Review
 ↓
Fix Feedback
 ↓
Merge
```

## 4. Commit message standard

Good:

```
feat: add trainee CRUD API
feat: implement JWT authentication
fix: validate duplicate trainee email
test: add trainee service tests
refactor: improve trainee service
```

Avoid: `update`, `changes`, `final`, `test`, `done`

## 5. Pull request standard

Every PR should include:

- Title
- Description
- Changes (bullet list)
- Testing notes
- Screenshots (for UI changes)
- Related issue

Example:

```
feat: implement trainee CRUD

Changes:
- Added trainee model
- Added trainee schema
- Added CRUD endpoints
- Added validation

Testing:
- Tested using Postman
- Added service tests
```
