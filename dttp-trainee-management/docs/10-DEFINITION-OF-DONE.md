# 10 — Definition of Done

## 1. Task completion chain

```
Code
 ↓
Validation
 ↓
Testing
 ↓
Documentation
 ↓
Git Commit
 ↓
Pull Request
 ↓
Mentor Review
 ↓
Feedback Fixed
 ↓
Approved
 ↓
Merged
```

A task is **complete only** when the full chain is finished.

## 2. Commit standard

Use conventional, descriptive messages:

```
feat: add trainee CRUD API
feat: implement JWT authentication
fix: validate duplicate trainee email
test: add trainee service tests
refactor: improve trainee service
```

Avoid vague commits (`update`, `changes`, `final`, `done`).

## 3. Pull request checklist

- [ ] Title describes the change
- [ ] Description includes **Changes** and **Testing**
- [ ] Related issue linked
- [ ] Screenshots attached for UI
- [ ] No secrets committed (`.env`, passwords, tokens)
- [ ] Branch is up to date with `development` (or agreed base)

## 4. Mentor review expectations

Reviewers check:

- Correctness vs acceptance criteria
- Validation and error handling
- Security basics (hashing, authz)
- Readability and structure
- Tests / evidence of testing
