# Web route coverage

This is the current smoke-test map for the Next.js App Router surface. The canonical suite is
[`web/e2e/smoke.spec.ts`](../web/e2e/smoke.spec.ts).

## How to run

```bash
cd web
pnpm typecheck
PORT=3200 pnpm test:e2e
pnpm build
```

Use `PORT=3200` or another free port when `3000` is already occupied. Playwright passes that
port through to both `next dev` and its `baseURL`.

The E2E suite blocks service workers and fulfills the small API fixture surface directly from
Playwright. That keeps route smoke deterministic and independent of MSW service-worker state,
while still exercising the real rendered pages, client guards, navigation targets, and dynamic
route params.

## Current verification

Last checked: 2026-06-04.

| Command | Result |
|---|---|
| `pnpm typecheck` | Passed |
| `pnpm build` | Passed |
| `PORT=3200 pnpm test:e2e` | Passed: 29 tests |

## Public routes

| Route | Coverage |
|---|---|
| `/` | Smoke asserts welcome screen renders |
| `/login` | Smoke asserts returning-fellow login renders |
| `/login?next=complete&email=...&token=...` | Smoke asserts enrolment-completion state renders |
| `/register` | Smoke asserts admission request form renders |
| `/verify?email=...` | Smoke asserts TAN entry screen renders |
| `/privacy` | Smoke asserts privacy policy renders |

## Participant routes

| Route | Coverage |
|---|---|
| `/events/join` | Smoke asserts session join screen renders |
| `/events/evt-neurips-2026-bkk` | Smoke asserts event home renders with fixture event |
| `/events/evt-neurips-2026-bkk/vicinity` | Smoke asserts vicinity list renders |
| `/events/evt-neurips-2026-bkk/chat/u-0002` | Smoke asserts chat page renders with fixture peer |
| `/matches` | Smoke asserts no-active-session state renders |
| `/matches/m-neurips-u-0002` | Smoke asserts match dossier renders |
| `/chats` | Smoke asserts thread list renders |
| `/interests` | Smoke asserts inquiries page renders |
| `/profile` | Smoke asserts profile page renders |
| `/profile/sns` | Smoke asserts SNS linking page renders |
| `/settings` | Smoke asserts settings/data controls render |
| `/me/register` | Smoke asserts personal register renders |

## Admin routes

Admin routes require an admin JWT in the app. The smoke suite seeds a test-local `SUPER_ADMIN`
token and fulfills `/api/admin/**` with Playwright fixtures.

| Route | Coverage |
|---|---|
| `/admin` | Smoke asserts registry overview renders |
| `/admin/events` | Smoke asserts event list renders fixture event |
| `/admin/events/new` | Smoke asserts event creation form renders |
| `/admin/events/evt-neurips-2026-bkk` | Smoke asserts event detail renders |
| `/admin/users` | Smoke asserts user list renders fixture user |
| `/admin/users/u-you-0001` | Smoke asserts user dossier renders |
| `/admin/audit` | Smoke asserts audit ledger renders fixture row |
| `/admin/ops` | Smoke asserts outbox/ops page renders fixture row |

## Not covered by route smoke

External OAuth provider round-trips for Facebook and LinkedIn are not exercised by route smoke
because they require provider app credentials and callback URLs. `/profile/sns` is still covered
at page-render level.
