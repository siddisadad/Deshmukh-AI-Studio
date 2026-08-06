# UI Wireframes
## AI Studio for Software Engineering — MVP

Text wireframes for core screens. Visual language: Linear/GitHub/Notion — dense, calm, minimal chrome. Support light and dark mode.

---

## 1. Design Tokens (guidance)

| Token | Guidance |
|---|---|
| Font | Distinct UI sans for product UI (e.g. IBM Plex Sans / Source Sans) — avoid Inter/Roboto defaults if branding a marketing site; app UI may use MUI theme typography consistently |
| Surface | Subtle layered backgrounds (not flat white only); soft borders |
| Accent | Single brand accent (teal/blue-green recommended — avoid purple-default AI cliché) |
| Radius | Modest (4–8px); no pill overload |
| Cards | Prefer lists/boards over card grids; cards only for interactive project tiles on dashboard |

---

## 2. Login

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│                     AI Studio                              │
│              Software Engineering Workspace                │
│                                                            │
│              ┌──────────────────────────┐                  │
│              │ Email                    │                  │
│              │ Password                 │                  │
│              │ [ Sign in ]              │                  │
│              │ Forgot password?         │                  │
│              │ Create account           │                  │
│              └──────────────────────────┘                  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## 3. Dashboard

```
┌──────┬─────────────────────────────────────────────────────┐
│ Logo │ Dashboard                          [Ada ▾] [Theme]  │
│      │                                                     │
├──────┤  Welcome back                                       │
│ Dash │                                                     │
│ Proj │  ┌──────────────┐ ┌──────────────┐ ┌────────────┐   │
│ Sett │  │ Client Portal│ │ Mobile API   │ │ + New      │   │
│      │  │ 12 req · 8 open tasks         │ │ Project    │   │
│      │  └──────────────┘ └──────────────┘ └────────────┘   │
│      │                                                     │
│      │  Recent activity                                    │
│      │  · BA improved “Password reset” · 2h ago            │
│      │  · Task moved to Review · 5h ago                    │
└──────┴─────────────────────────────────────────────────────┘
```

---

## 4. Project Workspace Shell

```
┌──────┬─────────────────────────────────────────────────────┐
│ Logo │ Client Portal (CP)                  [Search] [Ada]  │
├──────┼─────────────────────────────────────────────────────┤
│ Overview        │                                          │
│ Requirements    │   (page content)                         │
│ Tasks           │                                          │
│ AI Chat         │                                          │
│ Documents       │                                          │
│ Settings        │                                          │
└─────────────────┴──────────────────────────────────────────┘
```

Mobile: hamburger → drawer for project subnav.

---

## 5. Requirements

```
┌─ Requirements ──────────────────────────── [+ Add] ────────┐
│                                                            │
│  List (left ~40%)          Editor (right ~60%)             │
│  ┌──────────────────┐      Title: Password reset           │
│  │ ● Password reset │      Status [DRAFT▾] Priority [HIGH] │
│  │ ○ SSO login      │      ─────────────────────────────   │
│  │ ○ Billing webhook│      Description (markdown)          │
│  └──────────────────┘      ┌─────────────────────────────┐ │
│                            │ ...                         │ │
│                            └─────────────────────────────┘ │
│                            AI: [Improve] [Stories] [AC]    │
│                            ─ Improved ───────────────────  │
│                            (AI markdown, editable)         │
│                            ─ User stories ───────────────  │
│                            ─ Acceptance criteria ────────  │
└────────────────────────────────────────────────────────────┘
```

---

## 6. Tasks — Kanban

```
┌─ Tasks ────────────────────────── [Filter] [+ Task] ───────┐
│  TODO          IN PROGRESS      REVIEW           DONE      │
│ ┌──────────┐  ┌──────────┐    ┌──────────┐    ┌─────────┐ │
│ │ Reset API│  │ Email tpl│    │ AC review│    │ Schema  │ │
│ │ HIGH  auth│  │ MED      │    │ HIGH     │    │ DONE    │ │
│ └──────────┘  └──────────┘    └──────────┘    └─────────┘ │
│ ┌──────────┐                                               │
│ │ Docs README                                              │
│ └──────────┘                                               │
└────────────────────────────────────────────────────────────┘
```

Click task → right drawer: description, labels, assignee, requirement link, activity.

---

## 7. AI Chat

```
┌─ AI Chat ──────────────────────────────────────────────────┐
│ Assistants: ( BA )  Developer  QA  Docs                    │
│ limitations: Does not write production code                │
│────────────────────────────────────────────────────────────│
│  You · 10:02                                               │
│  Improve clarity of our auth requirements.                 │
│                                                            │
│  Business Analyst · 10:02                                  │
│  Based on project context (12 requirements, 8 tasks)...    │
│  1. ...                                                    │
│────────────────────────────────────────────────────────────│
│  [ Ask the Business Analyst…                      ] [Send] │
│  Context: shared project · Provider: mock                  │
└────────────────────────────────────────────────────────────┘
```

---

## 8. Documents

```
┌─ Documents ─────────────────────────────── [+ New] ────────┐
│  Sidebar list                 Editor                       │
│  README                       # Client Portal              │
│  API Doc                     ...                          │
│  Release notes                [Generate with Docs AI]      │
└────────────────────────────────────────────────────────────┘
```

---

## 9. Settings — Profile & Project

**Profile:** display name, email (read-only), theme, change password.  
**Project settings:** name, key, description, archive, members (ADMIN+).

---

## 10. Interaction Notes

| Interaction | Behavior |
|---|---|
| AI actions | Button → spinner on control → inline result; toast on failure |
| Kanban move | Optimistic UI; revert + error toast on failure |
| Empty states | Single CTA (“Add requirement”, “Ask an assistant”) |
| Dark mode | Respect profile theme; persist via PATCH `/me` |
| Motion | Subtle: page fade 150ms, drawer slide, AI result reveal — 2–3 purposeful motions max |

---

## 11. Accessibility

- Keyboard reachable Kanban status menu (if drag unavailable).
- Sufficient contrast in both themes.
- Focus rings on composer and primary CTAs.
- `aria-live` for AI completion announcements.

---

## 12. Document Control

| Version | Date | Notes |
|---|---|---|
| 1.0 | 2026-08-06 | Core MVP wireframes |

**Previous:** `07-AI-ARCHITECTURE.md` · **Next:** `09-DEVELOPMENT-ROADMAP.md`
