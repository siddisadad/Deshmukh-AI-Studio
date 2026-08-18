# 02 — System Architecture

## 1. Target architecture

```
                   ┌───────────────┐
                   │    Users      │
                   └───────┬───────┘
                           │
                           ▼
                   ┌───────────────┐
                   │ React Frontend│
                   └───────┬───────┘
                           │
                       REST / JSON
                           │
                           ▼
                   ┌───────────────┐
                   │   FastAPI     │
                   └───────┬───────┘
                           │
                    Business Logic
                           │
                           ▼
                   ┌───────────────┐
                   │  SQLAlchemy   │
                   └───────┬───────┘
                           │
                           ▼
                   ┌───────────────┐
                   │     MySQL     │
                   └───────────────┘
```

## 2. Request flow (learning objective)

```
HTTP Request
     ↓
FastAPI Router
     ↓
Python Service Function
     ↓
SQLAlchemy / MySQL
     ↓
Response (JSON)
```

Frontend path:

```
React Form
    ↓
POST /api/trainees
    ↓
FastAPI validation
    ↓
Service
    ↓
Database
    ↓
Response
    ↓
React UI update
```

## 3. Recommended backend layering

```
FastAPI
   ↓
Router
   ↓
Service
   ↓
SQLAlchemy
   ↓
MySQL
```

Suggested package layout (when coding begins):

```
backend/
├── app/
│   ├── main.py
│   ├── database/
│   │   ├── connection.py
│   │   └── base.py
│   ├── models/
│   ├── schemas/
│   ├── routers/
│   ├── services/
│   ├── auth/
│   └── config/
│       └── settings.py
├── tests/
├── requirements.txt
└── .env
```

## 4. Frontend structure (when coding begins)

```
frontend/src/
├── components/
├── pages/
├── layouts/
├── services/
├── hooks/
├── types/
├── utils/
├── routes/
└── App.tsx
```

## 5. Later platform pieces

| Piece | When |
|---|---|
| Docker Compose (React + API + MySQL) | After local app works |
| GitHub Actions (lint, test, build) | After app is stable |
| Deployment / monitoring | Month 3 |

Do **not** introduce CI/CD on Day 1. First make the application work locally.
