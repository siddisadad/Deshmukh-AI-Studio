# Frontend Folder Structure
## AI Studio for Software Engineering — MVP

| Field | Value |
|---|---|
| **Framework** | React 18+ / TypeScript |
| **UI** | Material UI (MUI) v5/v6 |
| **Routing** | React Router v6 |
| **Server state** | TanStack React Query |
| **Client state** | Zustand |
| **Build** | Vite |

---

## 1. Repository Layout

```
frontend/
├── package.json
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.ts
├── index.html
├── Dockerfile
├── nginx.conf                 # optional (or root nginx)
├── public/
│   └── favicon.svg
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── vite-env.d.ts
    ├── app/                   # app shell, router, providers
    ├── features/              # domain features
    ├── shared/                # reusable UI, lib, types
    ├── styles/
    └── assets/
```

---

## 2. Detailed `src/` Tree

```
src/
├── main.tsx
├── App.tsx
│
├── app/
│   ├── providers/
│   │   ├── AppProviders.tsx          # QueryClient, Theme, Auth
│   │   ├── ThemeProvider.tsx
│   │   └── AuthProvider.tsx
│   ├── router/
│   │   ├── index.tsx                 # createBrowserRouter
│   │   ├── ProtectedRoute.tsx
│   │   └── routes.tsx
│   └── layout/
│       ├── AppShell.tsx              # nav + content
│       ├── AuthLayout.tsx
│       ├── ProjectLayout.tsx         # project subnav
│       └── components/
│           ├── TopBar.tsx
│           ├── SideNav.tsx
│           └── ThemeToggle.tsx
│
├── features/
│   ├── auth/
│   │   ├── pages/
│   │   │   ├── LoginPage.tsx
│   │   │   ├── RegisterPage.tsx
│   │   │   └── ForgotPasswordPage.tsx
│   │   ├── api/authApi.ts
│   │   ├── hooks/useAuth.ts
│   │   ├── store/authStore.ts        # tokens + user (Zustand)
│   │   └── components/LoginForm.tsx
│   │
│   ├── dashboard/
│   │   ├── pages/DashboardPage.tsx
│   │   └── api/dashboardApi.ts
│   │
│   ├── projects/
│   │   ├── pages/
│   │   │   ├── ProjectsPage.tsx
│   │   │   ├── ProjectOverviewPage.tsx
│   │   │   └── ProjectSettingsPage.tsx
│   │   ├── api/projectsApi.ts
│   │   ├── components/
│   │   │   ├── ProjectCard.tsx
│   │   │   └── CreateProjectDialog.tsx
│   │   └── types.ts
│   │
│   ├── requirements/
│   │   ├── pages/RequirementsPage.tsx
│   │   ├── api/requirementsApi.ts
│   │   ├── components/
│   │   │   ├── RequirementEditor.tsx
│   │   │   ├── RequirementList.tsx
│   │   │   └── AiActionToolbar.tsx
│   │   └── hooks/useRequirementAi.ts
│   │
│   ├── tasks/
│   │   ├── pages/TasksPage.tsx
│   │   ├── api/tasksApi.ts
│   │   ├── components/
│   │   │   ├── KanbanBoard.tsx
│   │   │   ├── KanbanColumn.tsx
│   │   │   ├── TaskCard.tsx
│   │   │   └── TaskDrawer.tsx
│   │   └── types.ts
│   │
│   ├── chat/
│   │   ├── pages/AiChatPage.tsx
│   │   ├── api/chatApi.ts
│   │   ├── components/
│   │   │   ├── AssistantSelector.tsx
│   │   │   ├── MessageList.tsx
│   │   │   ├── MessageBubble.tsx
│   │   │   └── ChatComposer.tsx
│   │   └── hooks/useConversation.ts
│   │
│   ├── documents/
│   │   ├── pages/DocumentsPage.tsx
│   │   ├── api/documentsApi.ts
│   │   └── components/
│   │       ├── DocumentList.tsx
│   │       └── DocumentEditor.tsx
│   │
│   └── settings/
│       ├── pages/ProfileSettingsPage.tsx
│       └── api/profileApi.ts
│
├── shared/
│   ├── api/
│   │   ├── httpClient.ts             # fetch/axios + interceptors
│   │   ├── queryKeys.ts
│   │   └── types.ts                  # ApiError, PageResponse
│   ├── components/
│   │   ├── LoadingState.tsx
│   │   ├── EmptyState.tsx
│   │   ├── ErrorState.tsx
│   │   ├── ConfirmDialog.tsx
│   │   ├── Markdown.tsx
│   │   └── PriorityChip.tsx
│   ├── hooks/
│   │   ├── useDebouncedValue.ts
│   │   └── useLocalStorage.ts
│   ├── theme/
│   │   ├── palette.ts
│   │   ├── typography.ts
│   │   └── theme.ts                  # light/dark
│   ├── constants/
│   │   └── assistants.ts
│   └── utils/
│       ├── date.ts
│       └── string.ts
│
├── styles/
│   └── global.css
│
└── assets/
    └── logo.svg
```

---

## 3. Routing Map

| Path | Page | Guard |
|---|---|---|
| `/login` | LoginPage | Guest |
| `/register` | RegisterPage | Guest |
| `/forgot-password` | ForgotPasswordPage | Guest |
| `/dashboard` | DashboardPage | Auth |
| `/projects` | ProjectsPage | Auth |
| `/projects/:projectId` | ProjectOverviewPage | Auth + member |
| `/projects/:projectId/requirements` | RequirementsPage | Auth + member |
| `/projects/:projectId/tasks` | TasksPage | Auth + member |
| `/projects/:projectId/chat` | AiChatPage | Auth + member |
| `/projects/:projectId/documents` | DocumentsPage | Auth + member |
| `/projects/:projectId/settings` | ProjectSettingsPage | Auth + admin |
| `/settings/profile` | ProfileSettingsPage | Auth |

---

## 4. Authentication Flow (UI)

```
App load
  → read refresh strategy
  → if access token valid: hydrate user via GET /me
  → else try refresh
  → else redirect /login

Login/Register success
  → store tokens (memory + refresh persistence)
  → navigate /dashboard

401 from API
  → attempt single refresh
  → on failure: clear store → /login
```

`ProtectedRoute` checks `authStore.isAuthenticated`.  
`ProjectLayout` loads project; on 403/404 shows not-found.

---

## 5. State Management Rules

| Kind of state | Tool |
|---|---|
| Server data (projects, tasks, chat) | React Query |
| Auth session | Zustand (`authStore`) |
| UI chrome (sidebar open, theme) | Zustand or MUI ColorMode |
| Form ephemeral state | Local component state / RHF if needed |

Do **not** mirror server lists into Zustand.

---

## 6. HTTP Client

```ts
// shared/api/httpClient.ts — responsibilities
// - baseURL from import.meta.env.VITE_API_BASE_URL
// - attach Bearer access token
// - on 401: refresh once, retry
// - map errors to ApiError
```

React Query hooks wrap API modules (`features/*/api`).

---

## 7. UI/UX Implementation Notes

- **Shell:** left nav (Dashboard, Projects) + project subnav (Overview, Requirements, Tasks, AI Chat, Documents, Settings).
- **Kanban:** four columns; status change via PATCH; optimistic updates with rollback.
- **Requirement editor:** markdown textarea + AI action toolbar (Improve / Stories / AC) with loading states.
- **AI Chat:** assistant selector, message list, composer; show provider badge in footer.
- **Dark mode:** MUI `palette.mode` toggled from profile `theme` or system preference.
- **Responsive:** collapse side nav to drawer under `md` breakpoint.
- **Inspiration:** Linear density, GitHub project nav, Notion-like doc editor — avoid card spam in hero areas; workspace pages use clean lists and boards.

---

## 8. Environment

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

## 9. Scripts

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "lint": "eslint .",
    "test": "vitest"
  }
}
```

---

## 10. Testing Layout

```
src/
├── features/tasks/components/KanbanBoard.test.tsx
├── features/auth/pages/LoginPage.test.tsx
└── shared/api/httpClient.test.ts
```

Use Vitest + React Testing Library. MSW for API mocks.

---

## 11. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Feature-based React structure |

**Previous:** `05-BACKEND-STRUCTURE.md` · **Next:** `07-AI-ARCHITECTURE.md`
