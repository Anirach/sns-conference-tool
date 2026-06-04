import { expect, type Page, test } from "@playwright/test";

const EVENT_ID = "evt-neurips-2026-bkk";
const OTHER_USER_ID = "u-0002";
const MATCH_ID = "m-neurips-u-0002";
const ADMIN_USER_ID = "u-you-0001";

// The App Router dev server compiles routes lazily. Keeping this file serial avoids
// overwhelming `next dev` when the route matrix hits every page cold.
test.describe.configure({ mode: "serial" });

interface RouteSmoke {
  name: string;
  path: string;
  body: RegExp;
  admin?: boolean;
}

const publicRoutes: RouteSmoke[] = [
  { name: "welcome", path: "/", body: /Request Admission/i },
  { name: "login", path: "/login", body: /Good to see you\s+again/i },
  { name: "complete enrolment", path: "/login?next=complete&email=ada%40example.com&token=mock-token", body: /Confirm Enrolment/i },
  { name: "register", path: "/register", body: /Dispatch Cipher/i },
  { name: "verify", path: "/verify?email=ada%40example.com", body: /Transcribe the\s+six-digit\s+cipher/i },
  { name: "privacy", path: "/privacy", body: /What we keep,\s+and for how long/i }
];

const participantRoutes: RouteSmoke[] = [
  { name: "join event", path: "/events/join", body: /Demo Sessions/i },
  { name: "event home", path: `/events/${EVENT_ID}`, body: /NeurIPS 2026 Bangkok/i },
  { name: "event vicinity", path: `/events/${EVENT_ID}/vicinity`, body: /Intellectual\s*Affinities/i },
  { name: "event chat", path: `/events/${EVENT_ID}/chat/${OTHER_USER_ID}`, body: /Alice\s+Smith/i },
  { name: "matches without active event", path: "/matches", body: /No session in residence/i },
  { name: "match dossier", path: `/matches/${MATCH_ID}`, body: /Open Correspondence/i },
  { name: "chats", path: "/chats", body: /Recent\s+Letters/i },
  { name: "interests", path: "/interests", body: /Topics of\s+Inquiry/i },
  { name: "profile", path: "/profile", body: /Linked Societies/i },
  { name: "profile sns", path: "/profile/sns", body: /Facebook/i },
  { name: "settings", path: "/settings", body: /Export my dossier/i },
  { name: "personal register", path: "/me/register", body: /Everything we hold for you on this server/i }
];

const adminRoutes: RouteSmoke[] = [
  { name: "admin overview", path: "/admin", body: /Overview/i, admin: true },
  { name: "admin events", path: "/admin/events", body: /NeurIPS 2026 Bangkok/i, admin: true },
  { name: "admin new event", path: "/admin/events/new", body: /Coin a cipher/i, admin: true },
  { name: "admin event detail", path: `/admin/events/${EVENT_ID}`, body: /Venue heatmap/i, admin: true },
  { name: "admin users", path: "/admin/users", body: /you@example.com/i, admin: true },
  { name: "admin user dossier", path: `/admin/users/${ADMIN_USER_ID}`, body: /Recent ledger entries/i, admin: true },
  { name: "admin audit", path: "/admin/audit", body: /auth\.login\.success/i, admin: true },
  { name: "admin ops", path: "/admin/ops", body: /chat\.message/i, admin: true }
];

function collectBrowserErrors(page: Page): string[] {
  const errors: string[] = [];
  page.on("pageerror", (err) => errors.push(err.message));
  return errors;
}

function tokenWithRole(role: "USER" | "ADMIN" | "SUPER_ADMIN" = "SUPER_ADMIN"): string {
  const header = Buffer.from(JSON.stringify({ alg: "none", typ: "JWT" })).toString("base64url");
  const payload = Buffer.from(
    JSON.stringify({
      sub: ADMIN_USER_ID,
      role,
      iss: "playwright",
      aud: "sns-conf",
      exp: Math.floor(Date.now() / 1000) + 3600
    })
  ).toString("base64url");
  return `${header}.${payload}.`;
}

async function seedSession(page: Page, role: "USER" | "ADMIN" | "SUPER_ADMIN" = "SUPER_ADMIN") {
  const accessToken = tokenWithRole(role);
  await page.addInitScript(
    ({ jwt }) => {
      window.localStorage.setItem("sns.auth.jwt", jwt);
      window.localStorage.setItem("sns.auth.refresh", "playwright-refresh-token");
    },
    { jwt: accessToken }
  );
}

async function mockFrontendApi(page: Page) {
  const now = new Date().toISOString();
  const event = {
    eventId: EVENT_ID,
    eventName: "NeurIPS 2026 Bangkok",
    venue: "Queen Sirikit National Convention Center, Bangkok",
    expirationCode: now,
    qrCode: "NEURIPS2026",
    expired: false,
    attendanceCount: 20
  };
  const profile = {
    userId: ADMIN_USER_ID,
    email: "you@example.com",
    firstName: "Alex",
    lastName: "Chen",
    academicTitle: "PhD Candidate",
    institution: "ETH Zurich",
    profilePictureUrl: null
  };
  const match = {
    matchId: MATCH_ID,
    eventId: EVENT_ID,
    otherUserId: OTHER_USER_ID,
    name: "Alice Smith",
    title: "Prof.",
    institution: "MIT",
    profilePictureUrl: null,
    commonKeywords: ["graph-neural-networks", "privacy", "attention"],
    similarity: 0.87,
    mutual: true,
    distanceMeters: 21
  };
  const message = {
    messageId: "msg-1",
    eventId: EVENT_ID,
    fromUserId: OTHER_USER_ID,
    toUserId: ADMIN_USER_ID,
    content: "Are you presenting this afternoon?",
    readFlag: false,
    createdAt: now
  };

  await page.route(/\/api\//, async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const method = route.request().method();
    let status = 200;
    let payload: unknown = { ok: true };

    if (path === "/api/auth/register" && method === "POST") {
      status = 202;
      payload = { accepted: true };
    } else if (path === "/api/auth/verify" && method === "POST") {
      payload = { verified: true, verificationToken: "playwright-verification-token" };
    } else if (path === "/api/auth/complete" && method === "POST") {
      payload = { accessToken: tokenWithRole("USER"), refreshToken: "playwright-refresh-token", userId: ADMIN_USER_ID };
    } else if (path === "/api/auth/login" && method === "POST") {
      payload = { accessToken: tokenWithRole("USER"), refreshToken: "playwright-refresh-token", userId: ADMIN_USER_ID };
    } else if (path === "/api/auth/refresh" && method === "POST") {
      payload = { accessToken: tokenWithRole("USER"), refreshToken: "playwright-refresh-token", userId: ADMIN_USER_ID };
    } else if (path === "/api/events/joined") {
      payload = [event];
    } else if (path === "/api/events/join" && method === "POST") {
      payload = { event, joinedAt: now };
    } else if (path === `/api/events/${EVENT_ID}`) {
      payload = event;
    } else if (path === `/api/events/${EVENT_ID}/vicinity`) {
      payload = { radius: Number(url.searchParams.get("radius") ?? 50), matches: [match] };
    } else if (path === `/api/events/${EVENT_ID}/location`) {
      status = 204;
      payload = null;
    } else if (path === `/api/events/${EVENT_ID}/radius`) {
      payload = { ok: true };
    } else if (path === `/api/matches/${MATCH_ID}`) {
      payload = match;
    } else if (path === "/api/chats") {
      payload = [
        {
          threadId: `${EVENT_ID}:${OTHER_USER_ID}`,
          eventId: EVENT_ID,
          otherUserId: OTHER_USER_ID,
          otherName: "Alice Smith",
          otherInstitution: "MIT",
          otherPictureUrl: null,
          lastMessagePreview: message.content,
          lastMessageAt: now,
          lastFromMe: false,
          unread: 1
        }
      ];
    } else if (path === `/api/chat/${EVENT_ID}/${OTHER_USER_ID}`) {
      payload = {
        messages: [message],
        peer: {
          userId: OTHER_USER_ID,
          firstName: "Alice",
          lastName: "Smith",
          title: "Prof.",
          institution: "MIT",
          pictureUrl: null,
          commonKeywords: match.commonKeywords
        }
      };
    } else if (path === "/api/interests") {
      payload = [
        {
          interestId: "int-1",
          userId: ADMIN_USER_ID,
          type: "TEXT",
          content: "Graph neural networks for biomedical discovery",
          extractedKeywords: ["graph-neural-networks", "biomedical-ai"],
          createdAt: now
        }
      ];
    } else if (path === "/api/profile") {
      payload = profile;
    } else if (path === "/api/profile/settings") {
      payload = { pushMatches: true, pushChat: true, gpsConsent: true, keepRegister: false, language: "en" };
    } else if (path === "/api/sns") {
      payload = [];
    } else if (path === "/api/users/me/export") {
      await route.fulfill({
        status: 200,
        contentType: "application/zip",
        body: Buffer.from([0x50, 0x4b, 0x05, 0x06])
      });
      return;
    } else if (path === "/api/users/me" && method === "DELETE") {
      payload = { ok: true, status: "soft-deleted" };
    }

    await route.fulfill({
      status,
      contentType: "application/json",
      body: payload === null ? "" : JSON.stringify(payload)
    });
  });
}

async function mockAdminApi(page: Page) {
  const now = new Date().toISOString();
  const event = {
    eventId: EVENT_ID,
    name: "NeurIPS 2026 Bangkok",
    venue: "Queen Sirikit National Convention Center, Bangkok",
    qrCode: "NEURIPS2026",
    expirationCode: now,
    expired: false,
    participantCount: 20,
    centroidLat: 13.724,
    centroidLon: 100.56,
    matchCount: 163,
    messageCount: 19
  };
  const user = {
    userId: ADMIN_USER_ID,
    email: "you@example.com",
    firstName: "Alex",
    lastName: "Chen",
    academicTitle: "PhD Candidate",
    institution: "ETH Zurich",
    profilePictureUrl: null,
    role: "SUPER_ADMIN",
    suspended: false,
    deleted: false,
    createdAt: now,
    suspendedAt: null,
    deletedAt: null,
    interests: [
      {
        interestId: "int-1",
        type: "TEXT",
        content: "Graph neural networks for biomedical discovery",
        keywords: ["graph-neural-networks", "biomedical-ai"],
        createdAt: now
      }
    ],
    events: [{ eventId: EVENT_ID, name: event.name, joinedAt: now, selectedRadius: 50 }],
    matchCount: 15,
    chatMessageCount: 4,
    deviceCount: 1,
    snsLinkCount: 0,
    recentAudit: [
      {
        id: "audit-1",
        actorUserId: ADMIN_USER_ID,
        action: "auth.login.success",
        resourceType: "user",
        resourceId: ADMIN_USER_ID,
        payload: "{}",
        createdAt: now
      }
    ]
  };
  const metrics = {
    users: { total: 20, active: 20, suspended: 0, deleted24h: 0 },
    events: { active: 2, expired: 1 },
    outbox: { pending: 1, failed: 1, delivered24h: 8 },
    matches: { total: 163, created24h: 12 },
    audit24h: 22
  };
  const outboxRow = {
    outboxId: "outbox-1",
    userId: ADMIN_USER_ID,
    kind: "chat.message",
    status: "FAILED",
    attempts: 3,
    lastError: "demo failure",
    createdAt: now,
    deliveredAt: null
  };

  await page.route(/\/api\/admin(\/|$)/, async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    let payload: unknown = { ok: true };

    if (path === "/api/admin/ops/metrics") {
      payload = metrics;
    } else if (path === "/api/admin/events") {
      payload = { items: [event], total: 1, page: 0, size: 20 };
    } else if (path === `/api/admin/events/${EVENT_ID}`) {
      payload = event;
    } else if (path === `/api/admin/events/${EVENT_ID}/participants`) {
      payload = {
        items: [
          {
            userId: ADMIN_USER_ID,
            firstName: "Alex",
            lastName: "Chen",
            institution: "ETH Zurich",
            lastLat: 13.724,
            lastLon: 100.56,
            lastUpdate: now,
            selectedRadius: 50
          }
        ],
        total: 1,
        page: 0,
        size: 50
      };
    } else if (path === `/api/admin/events/${EVENT_ID}/heatmap`) {
      payload = [{ lat: 13.724, lon: 100.56, lastUpdate: now }];
    } else if (path === "/api/admin/users") {
      payload = { items: [user], total: 1, page: 0, size: 25 };
    } else if (path === `/api/admin/users/${ADMIN_USER_ID}`) {
      payload = user;
    } else if (path === "/api/admin/audit") {
      payload = { items: user.recentAudit, total: 1, page: 0, size: 50 };
    } else if (path === "/api/admin/ops/outbox") {
      payload = { items: [outboxRow], total: 1, page: 0, size: 50 };
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(payload)
    });
  });
}

async function expectRouteHealthy(page: Page, body: RegExp, errors: string[]) {
  await expect(page.locator("body")).toContainText(body);
  await expect(page.locator("body")).not.toContainText(/Unhandled Runtime Error|Application error|This page could not be found/i);
  expect(errors).toEqual([]);
}

test.describe("Pass 1 demo flow", () => {
  test.use({ serviceWorkers: "block" });

  test.beforeEach(async ({ context }) => {
    await context.grantPermissions(["geolocation"]);
    await context.setGeolocation({ latitude: 13.724, longitude: 100.56 });
  });

  test("register → verify → complete enrolment → join event", async ({ page }) => {
    const errors = collectBrowserErrors(page);
    await mockFrontendApi(page);
    await page.goto("/");
    await expect(page.getByRole("heading", { name: /Intellectual/i })).toBeVisible();

    await page.getByRole("link", { name: /Request Admission/i }).click();
    await expect(page).toHaveURL(/\/register$/);

    const email = `playwright+${Date.now()}@example.com`;
    await page.getByLabel("Email").fill(email);
    await page.getByRole("button", { name: /Dispatch Cipher/i }).click();

    await expect(page).toHaveURL(/\/verify\?/);
    const tanCells = page.locator('input[inputmode="numeric"]');
    await expect(tanCells).toHaveCount(6);
    for (let i = 0; i < 6; i++) {
      await tanCells.nth(i).fill(String((i + 1) % 10));
    }

    await expect(page).toHaveURL(/\/login\?.*next=complete/);
    await page.getByLabel("First name").fill("Ada");
    await page.getByLabel("Last name").fill("Lovelace");
    await page.getByLabel("Password").fill("analytical-engine");
    await page.getByRole("button", { name: /Confirm Enrolment/i }).click();

    await expect(page).toHaveURL(/\/interests$/);

    await page.goto("/events/join");
    await page.getByPlaceholder("NEURIPS2026").fill("NEURIPS2026");
    await page.getByRole("button", { name: /^Enter$/ }).click();

    await expect(page).toHaveURL(/\/events\/[^/]+$/);
    await expectRouteHealthy(page, /NeurIPS 2026 Bangkok/i, errors);
  });

  test("vicinity page renders matches from mock API", async ({ page }) => {
    const errors = collectBrowserErrors(page);
    await mockFrontendApi(page);
    await page.goto(`/events/${EVENT_ID}/vicinity`);
    await expectRouteHealthy(page, /Intellectual\s+Affinities/i, errors);
  });

  test("chats page loads threads", async ({ page }) => {
    const errors = collectBrowserErrors(page);
    await mockFrontendApi(page);
    await page.goto("/chats");
    await expect(page).toHaveURL(/\/chats$/);
    await expectRouteHealthy(page, /Recent\s+Letters/i, errors);
  });
});

test.describe("public route smoke coverage", () => {
  test.use({ serviceWorkers: "block" });

  test.beforeEach(async ({ context }) => {
    await context.grantPermissions(["geolocation"]);
    await context.setGeolocation({ latitude: 13.724, longitude: 100.56 });
  });

  for (const route of publicRoutes) {
    test(`${route.name} route renders`, async ({ page }) => {
      const errors = collectBrowserErrors(page);
      await page.goto(route.path);
      await expectRouteHealthy(page, route.body, errors);
    });
  }
});

test.describe("participant route smoke coverage", () => {
  test.use({ serviceWorkers: "block" });

  test.beforeEach(async ({ context }) => {
    await context.grantPermissions(["geolocation"]);
    await context.setGeolocation({ latitude: 13.724, longitude: 100.56 });
  });

  for (const route of participantRoutes) {
    test(`${route.name} route renders`, async ({ page }) => {
      const errors = collectBrowserErrors(page);
      await mockFrontendApi(page);
      await page.goto(route.path);
      await expectRouteHealthy(page, route.body, errors);
    });
  }
});

test.describe("admin route smoke coverage", () => {
  test.use({ serviceWorkers: "block" });

  test.beforeEach(async ({ context }) => {
    await context.grantPermissions(["geolocation"]);
    await context.setGeolocation({ latitude: 13.724, longitude: 100.56 });
  });

  for (const route of adminRoutes) {
    test(`${route.name} route renders`, async ({ page }) => {
      const errors = collectBrowserErrors(page);
      await seedSession(page, "SUPER_ADMIN");
      await mockAdminApi(page);
      await page.goto(route.path);
      await expectRouteHealthy(page, route.body, errors);
    });
  }
});
