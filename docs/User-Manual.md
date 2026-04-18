# SNS Conference Tool — User Manual

> A discreet register connecting researchers at conferences through curated affinities of inquiry.

This manual covers everything you can do in the SNS Conference Tool — both as a regular **fellow** (conference participant) and as an **admin** (event organiser, support, ops).

---

## Table of contents

1. [Quick start](#1-quick-start)
2. [Getting in](#2-getting-in)
   - 2.1 [First-time admission](#21-first-time-admission-register)
   - 2.2 [Returning fellow](#22-returning-fellow-login)
3. [The participant app](#3-the-participant-app)
   - 3.1 [The bottom tab bar](#31-the-bottom-tab-bar)
   - 3.2 [Discover — joining a session](#32-discover--joining-a-session)
   - 3.3 [Fellows — your match register](#33-fellows--your-match-register)
   - 3.4 [Letters — chat correspondence](#34-letters--chat-correspondence)
   - 3.5 [Study — settings, profile, interests](#35-study--settings-profile-interests)
4. [Interests — the engine of matching](#4-interests--the-engine-of-matching)
5. [Privacy & GDPR](#5-privacy--gdpr)
6. [The Registry — admin console](#6-the-registry--admin-console)
   - 6.1 [Becoming an admin](#61-becoming-an-admin)
   - 6.2 [Overview](#62-overview)
   - 6.3 [Sessions](#63-sessions)
   - 6.4 [Fellows](#64-fellows)
   - 6.5 [Ledger](#65-ledger)
   - 6.6 [Apparatus](#66-apparatus)
7. [Demo accounts](#7-demo-accounts)
8. [Troubleshooting](#8-troubleshooting)
9. [Glossary](#9-glossary)

---

## 1. Quick start

| You want to… | Do this |
|---|---|
| Try the app right now | Open http://localhost:3000 → **A Returning Fellow** → log in `you@example.com` / `Demo!2026` |
| Browse the conference floor | Tap **Discover** → tap **Join** on *NeurIPS 2026 Bangkok* |
| See who's nearby | Tap **Fellows** in the bottom bar → look at *Intellectual Affinities* |
| Send a message | Tap a fellow card → **Begin correspondence** |
| Manage events / users (admin) | Tap **Registry** in the bottom bar |

You can run all of this against the seeded demo dataset — no real conference required. See [Demo accounts](#7-demo-accounts).

---

## 2. Getting in

The app's "front door" is http://localhost:3000 in development (or your conference's published URL in production).

### 2.1 First-time admission (register)

1. Tap **Request Admission** on the welcome screen.
2. Enter your **work email** (e.g. `you@university.edu`).
3. Check your inbox (or MailHog at http://localhost:8025 in dev) for a six-digit **TAN**. In dev mode the TAN is always `123456`.
4. Enter the TAN on the verify screen → tap **Confirm**.
5. Fill in your **particulars**: first name, last name, academic title (Prof. / Dr. / PhD candidate / etc.), institution, and a password (≥ 8 characters, must not equal your email's local part, and must not be one of the embedded common-password blocklist).
6. Tap **Confirm Enrolment**. You're now logged in and dropped on the **Discover** screen.

### 2.2 Returning fellow (login)

1. Tap **A Returning Fellow** on the welcome screen.
2. Enter email + password.
3. Tap **Grant Admission**.

If your password is wrong you'll see *"Admission refused"*. The same generic error appears for an unknown email or a suspended account — by design, so attackers can't enumerate registered accounts.

> **Forgot your password?** v1 doesn't ship a password-reset flow. Contact your event organiser; they can suspend / reinstate or hard-delete your account in the **Registry** (see §6.4).

---

## 3. The participant app

After login you land on the **Discover** screen, the conference home.

### 3.1 The bottom tab bar

Persistent across every authenticated screen. Four tabs by default; admins see a fifth:

| Tab | Where it goes | What lives there |
|---|---|---|
| **Discover** | `/events/join` | Join a session by QR or code |
| **Fellows** | `/matches` | Your historical affinities (and the live vicinity per session) |
| **Letters** | `/chats` | All your active chat threads |
| **Study** | `/settings` | Settings, profile, interests, sign-out |
| **Registry** *(admins only)* | `/admin` | The management console (see §6) |

A **back arrow** appears on inner screens (e.g. *Match dossier*, *Chat with Jean Dupont*) so you always have one tap up the tree.

### 3.2 Discover — joining a session

A "session" is a conference event the organiser has set up (e.g. *NeurIPS 2026 Bangkok*).

**To join:**
- **Scan QR.** Tap **Scan Cipher** to open your camera and read the printed badge QR code at the venue.
- **Type the code.** Tap into the *Or transcribe* field, type the cipher (e.g. `NEURIPS2026`), tap **Enter**.
- **Demo Sessions list.** Below the scanner you'll see a list of known sessions. Tap **Join** on any active one to skip the QR step (useful for testing).

What happens on join:
- You're added to the session's roster as a **participant** with a default *50 m* visibility radius.
- Your **GPS** is requested (permission prompt) so the app can place you on the venue floor for vicinity matching. You can decline — vicinity will simply not work for that session.
- Once joined, the session becomes your **active session**; tapping **Fellows** now shows live nearby fellows.

You can be in multiple sessions at once. Switch between them via your *Joined* list (in v1, the most recently joined session is "active"; tap any other event card to swap).

**Adjourning a session.** Tap **Leave session** on the session detail screen. Your participation row is removed; you no longer appear in others' vicinity for that event. (You can re-join the same session at any time before its expiration.)

### 3.3 Fellows — your match register

The **Fellows** tab is the heart of the app. It lists everyone in your current session ranked by **intellectual affinity** (how similar your interest profile is to theirs) and, secondarily, by physical proximity.

**Top of the screen:**
- **Vol. IV — Issue II** eyebrow + *Intellectual Affinities* title.
- A **radius selector** (20 m / 50 m / 100 m) — controls how far the vicinity query reaches. Tap to change; results refresh immediately.
- A **By Affinity** dropdown — sort by *Affinity* (default), *Proximity* (closest first), or *Recency* (most recently seen).
- **Mutual / All** chips — *Mutual* hides one-way matches (where you appeared in their list but they don't yet appear in yours).
- An "**N within 50 m**" tally on the far right.

**Each fellow card shows:**
- Numbered position (`01`, `02`, …) in your list.
- **Portrait** (from their profile).
- **Title** in brass eyebrow caps (e.g. **ASSOC. PROF.**).
- **Name** (serif, large).
- **Institution** (serif italic).
- A short list of **shared keywords** (the overlap between your interests and theirs).
- A **distance** chip (e.g. *21M*).
- An **INDEX** percentage (cosine-similarity score, 0–100%).
- A coloured **bar** below the index showing the relative score visually.

**Tap a fellow** to open the **Match Dossier**:
- Larger portrait + full name + institution.
- The full list of common keywords (not truncated).
- Their public interests.
- A **Begin correspondence** button → opens a chat (see §3.4).

### 3.4 Letters — chat correspondence

The **Letters** tab lists every chat thread you have, most recent first.

**Each thread row shows:**
- Other fellow's portrait + name + institution.
- The last message preview (prefixed with **"You: "** if you sent it).
- Time delta (e.g. *about 2 hours*).
- An unread badge (small brass pill with a number) when there are messages addressed to you that you haven't read.

**Inside a thread:**
- Bubbles aligned right for your messages, left for theirs.
- Type your reply in the bottom composer → press Enter (or tap the send button).
- Messages are delivered in real time over a STOMP WebSocket — the other party sees them appear without refreshing.
- When you open a thread, all messages addressed to you are marked read; the unread badge clears.

**Push notifications** (mobile only): if you've granted notification permission, you'll get an OS notification when a new message arrives while the app is in the background.

### 3.5 Study — settings, profile, interests

Tap **Study** in the bottom bar to reach your settings page. From here:

- **Profile** → edit first name, last name, academic title, institution, profile portrait.
- **Interests** → add / remove the interests that drive matching (see §4).
- **SNS Links** → optionally connect your Facebook or LinkedIn account so the system can enrich your profile (academic position, affiliation history) automatically.
- **Push notifications** → toggle on / off.
- **GPS consent** → toggle on / off (turning off disables vicinity matching).
- **Local storage** → toggle whether the mobile shell can cache offline data.
- **Language** → English / Thai / German.
- **Sign out** → logs you out and clears your session.
- **Export my data** → downloads a ZIP containing every record we have about you (see §5).
- **Delete my account** → schedules your account for permanent deletion in 30 days (see §5).

---

## 4. Interests — the engine of matching

Matching is a **cosine similarity** between your *keyword vector* and every other fellow's, scoped to the session you're in. To produce useful matches the system needs to know what you're interested in.

You can add interests three ways:

1. **Free text.** Type a sentence or paragraph (e.g. *"I work on graph neural networks for drug-target interaction prediction, focusing on heterogeneous graphs and explainability."*). The keyword extractor (TF or OpenNLP if configured) pulls out the concept-bearing terms and weights them.
2. **Article link.** Paste a URL (e.g. an arXiv abstract). The system stores the link and the descriptive text you provide alongside it.
3. **Article upload.** Upload a PDF or plain-text article. The text is extracted and run through the same keyword pipeline.

**Practical advice:**
- **Quality > quantity.** Two or three rich, specific paragraphs produce better matches than a long list of single keywords.
- **Use natural language.** "Federated learning, privacy, distributed training" is fine, but "*Federated learning for medical imaging with strict privacy guarantees over hospital networks*" is much better — the extractor uses bigram phrases too.
- **Edit anytime.** Removing or adding interests triggers an automatic re-computation of your matches for every session you've joined.

See your extracted keywords in the *Interests* screen — each interest card lists the keywords it contributed to your vector.

---

## 5. Privacy & GDPR

The system is built for compliance:

- **Locations** are only stored as long as a session is active and only at coarse precision (~1 m).
- **IP addresses** are SHA-256-hashed with a per-deployment salt before being written to the audit log — raw IPs never persist.
- **Audit log** is append-only enforced by a Postgres trigger and pruned automatically after 180 days.
- **Encrypted SNS tokens** are AES-256-GCM at rest; if you unlink, they're zeroed.

**Your rights:**

| Action | Where | Effect |
|---|---|---|
| **Export everything** | Study → Export my data | Streams a ZIP: `profile.json`, `interests.json`, `matches.json`, `chat-threads.json`, `chat-messages.json`, `sns-links.json`, `manifest.json`. |
| **Soft-delete** | Study → Delete my account | Sets `deleted_at = now()`. You're immediately logged out and can no longer log in. |
| **Hard-delete** | (automatic, after 30 days) | A scheduled cron permanently removes every row tied to your `user_id` (cascades wipe profile, interests, participations, matches, chat, devices, refresh tokens, SNS links). |

You can also ask an admin to suspend or hard-delete you immediately — see §6.4.

---

## 6. The Registry — admin console

The admin console (route `/admin`) is the operator's view: create events, manage users, search the audit log, monitor the push pipeline. It's only accessible to users with role `ADMIN` or `SUPER_ADMIN`.

The console **shares the participant chrome** — same cream theme, same top app bar, same bottom tab bar. The only structural difference is a horizontal **section nav** of pills below the page header (Overview · Sessions · Fellows · Ledger · Apparatus) for jumping between admin sub-sections.

### 6.1 Becoming an admin

There's no self-service path. You become an admin one of three ways:

1. **You're the seeded demo super-admin.** `you@example.com` ships as `SUPER_ADMIN`; `lukas.svensson@kth.se` and `rajesh.iyer@stanford.edu` ship as `ADMIN`. Useful for trying the console end-to-end.
2. **An operator promoted you.** Any existing `SUPER_ADMIN` can change your role on the *Fellows* admin screen (see §6.4).
3. **You're the bootstrap admin.** Setting the env var `SNS_ADMIN_EMAIL=you@whatever.com` and rebooting the backend automatically promotes that account to `SUPER_ADMIN` on every boot. (In production, `ProductionSecretsCheck` refuses to start without it.)

Once your role is `ADMIN` or `SUPER_ADMIN`, the bottom tab bar grows from 4 to 5 tabs the next time you log in. Tap **Registry** to enter the console.

### 6.2 Overview

`/admin`. Tile-grid dashboard, refreshes every 30 seconds.

| Section | Tiles |
|---|---|
| **Fellows** | Total · Active · Suspended · Deleted (24 h) |
| **Sessions** | Active · Expired · Affinities · New (24 h) |
| **Apparatus** | Outbox pending · Outbox failed · Delivered (24 h) · Audit (24 h) |

Tiles turn **amber** when the value enters a warning band (e.g. > 50 outbox pending) and **red** when failure (e.g. any outbox FAILED). Two link cards at the bottom shortcut to *Sessions* and *Fellows*.

### 6.3 Sessions

`/admin/events`. The conference-organiser surface.

**List view:**
- Paged table: Session name + venue · Cipher (QR plaintext) · Status (*In residence* / *Adjourned*) · Fellows count.
- **Search box** above the table — substring matches name, venue, or cipher.
- **+ New** button → create form (see below).
- Tap a row → session detail.

**Create new session** (`/admin/events/new`):
- **Session name** — display name (e.g. *NeurIPS 2027 Singapore*).
- **Venue** — free-text venue + city (e.g. *Marina Bay Sands*).
- **Cipher** — short uppercase code that becomes the QR text (e.g. `NEURIPS2027`). Refused with **Cipher already in use** if it collides with another active session.
- **Adjourns at** — datetime when the session expires (after which fellows can't join).
- **Centroid lat/lon** *(optional)* — sets the venue origin for the heatmap. If omitted, the heatmap centres on the geometric mean of fellows' positions.
- Tap **Create session** → you're redirected to the new session detail page with its cipher ready to print.

**Session detail** (`/admin/events/[id]`):
- **Header**: name + venue + cipher.
- **Stat tiles**: Fellows in residence · Affinities · Correspondence (chat messages) · Status.
- **Venue heatmap**: inline SVG showing concentric 10/25/50 m rings centred on the venue with each fellow's position dotted. Useful for spotting clusters or stragglers.
- **Fellows present** table: name + institution + last GPS fix time + selected radius. Tap a row to jump to that fellow's dossier.
- **Adjourn permanently** button (at the bottom): tapping it shows a confirmation; confirming hard-deletes the event and cascades to participations, matches, and chat messages. *This cannot be undone.*

### 6.4 Fellows

`/admin/users`. The user-admin surface.

**List view:**
- Paged table: Name + email + institution · Role · Status (*active* / *suspended* / *deleted*).
- **Search by email** + two filter selectors (**All roles** / All / Organizer / Admin / Super admin) and (**Any status** / Active / Suspended / Deleted).
- Tap a row → user dossier.

**Dossier** (`/admin/users/[id]`):
- **Header**: portrait, full name, email, academic title, institution.
- **Stat tiles**: Status · Role · Interests · Affinities · Messages · Sessions.
- **Role chips**: tap any role to change; greyed out if it's already the current role. SUPER_ADMIN-only — refuses if you're not super-admin or if it would demote the last super-admin.
- **Interests** list: every interest the user has registered, with the keywords it contributed.
- **Sessions joined** list: every session they're a participant in, with their selected radius and join date.
- **Recent ledger entries** (latest 50) — all audit-log rows where they were the actor.
- **Action buttons** at the bottom:
  - **Suspend** / **Reinstate** — toggle `suspended_at`. Suspended users get the same generic 401 on login as bad-password; the audit log distinguishes via `auth.login.suspended`.
  - **Soft delete** — sets `deleted_at = now()`. The user can no longer log in; the scheduled hard-delete cron will permanently wipe them after 30 days.
  - **Hard delete** — two-tap (first tap shows *Confirm hard delete*). SUPER_ADMIN-only. Refuses to wipe the last super-admin.

### 6.5 Ledger

`/admin/audit`. Read-only search over the immutable `audit_log` table (a Postgres trigger blocks UPDATE / DELETE outside the prune job).

**Filters** (all optional):
- **Actor user id** — paste a UUID (e.g. from a fellow dossier) to see everything that user did.
- **Action** — exact match against the action string (e.g. `auth.login.failure`, `admin.user.suspended`).
- **Since** — datetime lower bound; only rows newer than this.

**Each row shows:**
- *When* (local timestamp).
- *Action* code + the actor's user id (first 8 chars).

The list pages 50 at a time. The 24-hour total appears as a tile on the **Overview** and **Apparatus** screens.

**Common actions:**

| Code | Meaning |
|---|---|
| `auth.register` / `auth.verify` / `auth.complete` | Registration funnel |
| `auth.login` / `auth.login.failure` / `auth.login.suspended` / `auth.login.unverified` | Login outcomes |
| `auth.refresh` / `auth.refresh.reuse_detected` | Refresh-token rotation; reuse_detected = potential token theft |
| `auth.logout` | Clean logout |
| `profile.update` / `profile.soft_delete` / `profile.hard_delete` | Profile lifecycle |
| `sns.link` / `sns.callback` / `sns.unlink` / `sns.enrich` | SNS pipeline |
| `export.download` | GDPR export pulled |
| `admin.event.created` / `admin.event.updated` / `admin.event.deleted` | Event admin |
| `admin.user.suspended` / `admin.user.unsuspended` / `admin.user.role_changed` / `admin.user.soft_deleted` / `admin.user.hard_deleted` | User admin |
| `admin.outbox.retry` | Manual outbox retry |

### 6.6 Apparatus

`/admin/ops`. The push-pipeline + system-health surface.

**Top tiles** (same as Overview's Apparatus row):
- **Outbox pending** — messages waiting to be sent. Amber if > 50.
- **Outbox failed** — terminal failures (already retried 5 times). Red if > 0.
- **Delivered (24 h)** — successfully pushed in the last day.
- **Audit (24 h)** — total audit rows in the last day.

**Filter chips** above the table: **all** · **PENDING** · **FAILED** · **DELIVERED**. Tap to switch.

**Outbox table**:
- *Kind* — the message type (e.g. `match_found`, `chat_message`).
- *Status* + attempt count.
- *Retry* link (only on FAILED rows) — flips the row back to PENDING with `attempts = 0`; the existing scheduled drain picks it up within ~5 seconds.

The refresh icon in the header forces an immediate reload of both metrics and the outbox table.

---

## 7. Demo accounts

Out of the box (the seeded dev dataset):

| Email | Password | Role | Notes |
|---|---|---|---|
| `you@example.com` | `Demo!2026` | SUPER_ADMIN | Alex Chen, ETH Zurich. Already joined NeurIPS + ACL with interests + chat history. |
| `lukas.svensson@kth.se` | `Demo!2026` | ADMIN | Lukas Svensson, KTH Stockholm. |
| `rajesh.iyer@stanford.edu` | `Demo!2026` | ADMIN | Rajesh Iyer, Stanford University. |
| `alice.smith@mit.edu` | `Demo!2026` | USER | Alice Smith, MIT. Use this to test what a regular fellow sees. |
| (any of the other 16 seeded fellows) | `Demo!2026` | USER | All seeded with the same password. |

The seeder runs once on a fresh DB volume — it short-circuits on subsequent boots when `you@example.com` already exists. To re-seed, wipe the volume:

```bash
docker compose -f infra/docker-compose.dev.yml --profile backend --profile web down -v
docker compose -f infra/docker-compose.dev.yml --profile backend --profile web up -d --build
```

---

## 8. Troubleshooting

### "Admission refused" with the right password

Almost always one of:
1. **The backend was restarted** and it rotated its ephemeral JWT keypair (dev only — prod uses persistent keys). The frontend tries to refresh with the stale token and gets 400. **Fix**: clear browser localStorage (DevTools → Application → Storage → Clear site data) and log in again.
2. **You hit the rate limit** — login allows 30 attempts per IP per hour and 10 per email per hour. **Fix**: wait, or restart the backend (`docker restart sns-backend`) to drop the in-memory limiter.
3. **Your account was suspended.** A super-admin needs to reinstate you in the Registry → Fellows screen.

### Fellows page is blank after joining

- **Wait 30 seconds.** The matching engine recomputes asynchronously after you join; vicinity refreshes every 30 seconds.
- **Widen the radius** to 100 m.
- **Add interests.** Without interests you have no keyword vector — no peer can have a non-zero similarity with you.
- **Check your GPS.** Without a recent location fix (within 5 minutes) the vicinity query won't include you. Check **Study → GPS consent** is on.

### Chat messages don't arrive in real time

- The websocket auth needs your JWT — sign out and back in if your session is stale.
- Page reload often kicks the STOMP client back into a working state.
- The `/chats` thread list refreshes every 15 seconds even without WS, so you'll see new messages on a refresh.

### "Unable to render rich display" in a Mermaid diagram

That's a markdown-renderer error in the source docs (README), not your account. Check the README on GitHub directly.

---

## 9. Glossary

| Term | Meaning |
|---|---|
| **Admission** | The login screen ("A Returning Fellow"). |
| **Adjourn** | Either *let a session expire* (passive) or *delete a session* (admin action). |
| **Affinity** | The cosine-similarity score between two fellows' interest vectors. |
| **Apparatus** | The admin ops dashboard (push outbox + system health). |
| **Cipher** | The plaintext QR code that joins a session (e.g. `NEURIPS2026`). |
| **Correspondence** | A chat thread between two fellows in the same session. |
| **Discover** | The participant home screen — list of joinable sessions. |
| **Dossier** | The detailed view of a single fellow on the admin Fellows screen. |
| **Fellow** | Any registered user. From an admin's perspective, the term covers regular USERs, ORGANIZERs, ADMINs, and SUPER_ADMINs. |
| **Index** | A fellow's affinity score expressed as a 0–100% percentage. |
| **In residence** | The session's status while it's active (not adjourned). |
| **Ledger** | The admin audit-log explorer. |
| **Letters** | The participant chat inbox tab. |
| **Particulars** | Your profile data (name, title, institution, portrait). |
| **Registry** | The admin console as a whole; also the name of the bottom tab that opens it. |
| **Session** | A single conference event (NeurIPS Bangkok, ACL Vienna, …). |
| **Study** | The participant settings tab. |
| **TAN** | The six-digit transaction number used to verify your email at registration. |
| **Vicinity** | The set of fellows currently within your selected radius (20 / 50 / 100 m). |

---

*Last updated 2026-04-18. For developer documentation see [README.md](../README.md), for security guarantees see [SECURITY.md](SECURITY.md), for the canonical product spec see [SNS-system.md](SNS-system.md).*
