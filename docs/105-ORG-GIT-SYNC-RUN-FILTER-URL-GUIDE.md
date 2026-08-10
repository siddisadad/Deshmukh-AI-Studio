# Organization Git Sync Run Filter URL Guide

**Version:** v0.2.100-beta

Bookmark and share org-wide git sync run history filter state via URL query parameters.

## 1. URL parameters

Settings → **Git** at `/settings/git` syncs active org sync run filters to the query string (alongside overview params):

| Param | Values | Maps to run filter |
|---|---|---|
| `runSource` | `manual`, `scheduled`, `webhook` | Source dropdown |
| `runStatus` | `success`, `failed` | Status dropdown |
| `runProject` | project UUID | Project dropdown |

Omitted params mean “all” for that dimension. Dropdowns update the URL; opening a link with these params applies run filters on load and scrolls to the runs section.

Example:

`/settings/git?runStatus=failed&runProject={projectId}`

Combined with overview filters:

`/settings/git?linked=linked&enabled=enabled&lastSync=failed&runStatus=failed`

## 2. UI

- **Copy filtered link** on the runs section when any run filter is active
- **Clear filters** removes run params from the URL
- Overview row **View failed runs** applies `runStatus=failed` + project and updates the URL

## 3. Related

- Run project filter: [86-ORG-GIT-SYNC-RUN-PROJECT-FILTER-GUIDE.md](86-ORG-GIT-SYNC-RUN-PROJECT-FILTER-GUIDE.md)
- Overview filter URL: [103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md](103-ORG-GIT-SYNC-OVERVIEW-FILTER-URL-GUIDE.md)
