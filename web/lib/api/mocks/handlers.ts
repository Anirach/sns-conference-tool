import { http, HttpResponse, delay, type HttpHandler } from "msw";
import {
  CURRENT_USER_ID,
  allUsers,
  currentUser,
  events,
  eventsByCode,
  findEvent,
  findMatchById,
  findMatchPair,
  findUser,
  interestsForCurrentUser,
  listThreads,
  matchesForEvent,
  chatForPair,
  appendChatMessage,
  type ChatMessage,
  type Interest,
  type SnsLink,
  type User
} from "../../fixtures";

const BASE = "/api";

const joinedEventIds = new Set<string>();
const snsLinks: SnsLink[] = [];
const userInterests: Interest[] = [...interestsForCurrentUser];
const profileState: User = { ...currentUser };
let eventRadius = 50;

export type MockDomain =
  | "auth"
  | "admin"
  | "profile"
  | "sns"
  | "interests"
  | "events"
  | "matches"
  | "chat"
  | "users"
  | "devices"
  | "account";

interface MockSettings {
  pushMatches: boolean;
  pushChat: boolean;
  gpsConsent: boolean;
  keepRegister: boolean;
  language: "en" | "th" | "de";
}

const userSettingsState: MockSettings = {
  pushMatches: true,
  pushChat: true,
  gpsConsent: true,
  keepRegister: false,
  language: "en"
};

export const authHandlers = [
  http.post(`${BASE}/auth/register`, async () => {
    await delay(250);
    return HttpResponse.json({ accepted: true }, { status: 202 });
  }),

  http.post(`${BASE}/auth/verify`, async () => {
    await delay(250);
    return HttpResponse.json({ verified: true, verificationToken: "mock-verification-token" });
  }),

  http.post(`${BASE}/auth/complete`, async ({ request }) => {
    await delay(250);
    const body = (await request.json()) as { firstName?: string; lastName?: string; academicTitle?: string; institution?: string };
    Object.assign(profileState, {
      firstName: body.firstName ?? profileState.firstName,
      lastName: body.lastName ?? profileState.lastName,
      academicTitle: body.academicTitle ?? profileState.academicTitle,
      institution: body.institution ?? profileState.institution
    });
    return HttpResponse.json({
      accessToken: "mock.access.jwt",
      refreshToken: "mock.refresh.jwt",
      userId: CURRENT_USER_ID
    });
  }),

  http.post(`${BASE}/auth/login`, async () => {
    await delay(250);
    return HttpResponse.json({
      accessToken: "mock.access.jwt",
      refreshToken: "mock.refresh.jwt",
      userId: CURRENT_USER_ID
    });
  }),

  http.post(`${BASE}/auth/refresh`, async () => {
    await delay(100);
    return HttpResponse.json({
      accessToken: "mock.access.jwt.refreshed",
      refreshToken: "mock.refresh.jwt.refreshed",
      userId: CURRENT_USER_ID
    });
  }),

  http.post(`${BASE}/auth/logout`, async () => HttpResponse.json({ ok: true }))
];

export const profileHandlers = [
  http.get(`${BASE}/profile`, async () => {
    await delay(150);
    return HttpResponse.json(profileState);
  }),

  http.put(`${BASE}/profile`, async ({ request }) => {
    const body = (await request.json()) as Partial<User>;
    Object.assign(profileState, body);
    await delay(200);
    return HttpResponse.json(profileState);
  }),

  http.get(`${BASE}/profile/settings`, async () => {
    await delay(80);
    return HttpResponse.json(userSettingsState);
  }),

  http.put(`${BASE}/profile/settings`, async ({ request }) => {
    const body = (await request.json()) as Partial<MockSettings>;
    if (body.pushMatches !== undefined)  userSettingsState.pushMatches = body.pushMatches;
    if (body.pushChat !== undefined)     userSettingsState.pushChat = body.pushChat;
    if (body.gpsConsent !== undefined)   userSettingsState.gpsConsent = body.gpsConsent;
    if (body.keepRegister !== undefined) userSettingsState.keepRegister = body.keepRegister;
    if (body.language !== undefined)     userSettingsState.language = body.language;
    await delay(120);
    return HttpResponse.json(userSettingsState);
  })
];

export const snsHandlers = [
  http.get(`${BASE}/sns`, async () => {
    await delay(100);
    return HttpResponse.json(snsLinks);
  }),

  // Real backend returns {authUrl, state} so the popup can redirect to the provider.
  // Mock returns the same shape — the popup target is harmless in frontend-only dev.
  http.post(`${BASE}/sns/link`, async ({ request }) => {
    const body = (await request.json()) as { provider: "FACEBOOK" | "LINKEDIN" };
    const state = `mock-state-${Date.now()}`;
    await delay(150);
    return HttpResponse.json({
      authUrl: `https://example.com/mock-oauth/${body.provider.toLowerCase()}?state=${state}`,
      state
    });
  }),

  // Pretends the OAuth round-trip succeeded; appends a SnsLink so the list re-fetch shows it.
  http.post(`${BASE}/sns/callback`, async ({ request }) => {
    const body = (await request.json()) as { provider: "FACEBOOK" | "LINKEDIN" };
    const existing = snsLinks.findIndex((l) => l.provider === body.provider);
    const link: SnsLink = {
      provider: body.provider,
      providerUserId: `mock-${body.provider.toLowerCase()}-uid`,
      linkedAt: new Date().toISOString()
    };
    if (existing >= 0) snsLinks[existing] = link;
    else snsLinks.push(link);
    await delay(200);
    return HttpResponse.json({ ok: true });
  }),

  http.delete(`${BASE}/sns/:provider`, async ({ params }) => {
    const idx = snsLinks.findIndex((l) => l.provider === params.provider);
    if (idx >= 0) snsLinks.splice(idx, 1);
    await delay(150);
    return HttpResponse.json({ ok: true });
  })
];

// GDPR export + self soft-delete — backed by real endpoints; mock returns plausible shapes.
export const accountHandlers = [
  http.get(`${BASE}/users/me/export`, async () => {
    await delay(300);
    // Empty ZIP signature bytes — enough for a frontend "Save As" round-trip to succeed.
    return new HttpResponse(
      new Blob([new Uint8Array([0x50, 0x4b, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])], {
        type: "application/zip"
      }),
      { status: 200, headers: { "Content-Type": "application/zip" } }
    );
  }),

  http.delete(`${BASE}/users/me`, async () => {
    await delay(200);
    return HttpResponse.json({ ok: true, status: "soft-deleted" });
  })
];

export const interestsHandlers = [
  http.get(`${BASE}/interests`, async () => {
    await delay(150);
    return HttpResponse.json(userInterests);
  }),

  http.post(`${BASE}/interests`, async ({ request }) => {
    const body = (await request.json()) as { type: Interest["type"]; content: string };
    const keywords = extractKeywordsMock(body.content);
    const created: Interest = {
      interestId: `int-${Date.now()}`,
      userId: CURRENT_USER_ID,
      type: body.type,
      content: body.content,
      extractedKeywords: keywords,
      createdAt: new Date().toISOString()
    };
    userInterests.unshift(created);
    await delay(600);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.delete(`${BASE}/interests/:id`, async ({ params }) => {
    const idx = userInterests.findIndex((i) => i.interestId === params.id);
    if (idx >= 0) userInterests.splice(idx, 1);
    return HttpResponse.json({ ok: true });
  })
];

export const eventsHandlers = [
  http.get(`${BASE}/events/joined`, async () => {
    await delay(120);
    return HttpResponse.json(
      events.filter((e) => joinedEventIds.has(e.eventId)).map(withAttendance)
    );
  }),

  http.get(`${BASE}/events/:eventId`, async ({ params }) => {
    const evt = findEvent(params.eventId as string);
    if (!evt) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json(withAttendance(evt));
  }),

  http.post(`${BASE}/events/join`, async ({ request }) => {
    const body = (await request.json()) as { eventCode: string };
    const evt = eventsByCode[body.eventCode?.toUpperCase()];
    if (!evt) {
      return HttpResponse.json(
        { type: "/problems/not-found", title: "Event not found", status: 404 },
        { status: 404 }
      );
    }
    if (evt.expired) {
      return HttpResponse.json(
        { type: "/problems/validation", title: "Event expired", status: 400 },
        { status: 400 }
      );
    }
    joinedEventIds.add(evt.eventId);
    await delay(300);
    return HttpResponse.json({ event: withAttendance(evt), joinedAt: new Date().toISOString() });
  }),

  http.post(`${BASE}/events/:eventId/leave`, async ({ params }) => {
    joinedEventIds.delete(params.eventId as string);
    return HttpResponse.json({ ok: true });
  }),

  http.get(`${BASE}/events/:eventId/vicinity`, async ({ params, request }) => {
    const url = new URL(request.url);
    const radius = Number(url.searchParams.get("radius") ?? eventRadius);
    const matches = matchesForEvent(params.eventId as string, radius);
    await delay(250);
    return HttpResponse.json({ radius, matches });
  }),

  http.post(`${BASE}/events/:eventId/location`, async () => {
    return new HttpResponse(null, { status: 204 });
  }),

  http.put(`${BASE}/events/:eventId/radius`, async ({ request }) => {
    const body = (await request.json()) as { radius: number };
    eventRadius = body.radius;
    return HttpResponse.json({ ok: true });
  })
];

export const matchesHandlers = [
  http.get(`${BASE}/matches/:matchId`, async ({ params }) => {
    const m = findMatchById(params.matchId as string);
    if (!m) return new HttpResponse(null, { status: 404 });
    await delay(120);
    return HttpResponse.json(m);
  })
];

export const chatHandlers = [
  http.get(`${BASE}/chats`, async () => {
    await delay(150);
    return HttpResponse.json(listThreads());
  }),

  http.get(`${BASE}/chat/:eventId/:otherUserId`, async ({ params }) => {
    const eventId = params.eventId as string;
    const otherUserId = params.otherUserId as string;
    const messages = chatForPair(eventId, otherUserId);
    const peer = findUser(otherUserId);
    const match = findMatchPair(eventId, otherUserId);
    await delay(150);
    return HttpResponse.json({
      messages,
      peer: {
        userId: otherUserId,
        firstName: peer?.firstName ?? null,
        lastName: peer?.lastName ?? null,
        title: peer?.academicTitle ?? null,
        institution: peer?.institution ?? null,
        pictureUrl: peer?.profilePictureUrl ?? null,
        commonKeywords: match?.commonKeywords ?? []
      }
    });
  }),

  http.post(`${BASE}/chat/send`, async ({ request }) => {
    const body = (await request.json()) as { eventId: string; toUserId: string; content: string };
    const msg: ChatMessage = {
      messageId: `msg-client-${Date.now()}`,
      eventId: body.eventId,
      fromUserId: CURRENT_USER_ID,
      toUserId: body.toUserId,
      content: body.content,
      readFlag: false,
      createdAt: new Date().toISOString()
    };
    appendChatMessage(msg);
    return HttpResponse.json(msg, { status: 201 });
  }),

  http.post(`${BASE}/chat/read`, async () => HttpResponse.json({ ok: true }))
];

export const usersHandlers = [
  http.get(`${BASE}/users/:id`, async ({ params }) => {
    const u = findUser(params.id as string);
    if (!u) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json(u);
  }),

  http.get(`${BASE}/users`, async () => HttpResponse.json(allUsers))
];

export const devicesHandlers = [
  http.post(`${BASE}/devices/register`, async () => HttpResponse.json({ ok: true }))
];

export const adminHandlers = [
  http.get(`${BASE}/admin/ops/metrics`, async () => {
    await delay(100);
    return HttpResponse.json({
      users: { total: allUsers.length, active: allUsers.length, suspended: 0, deleted24h: 0 },
      events: {
        active: events.filter((e) => !e.expired).length,
        expired: events.filter((e) => e.expired).length
      },
      outbox: { pending: 1, failed: 1, delivered24h: 8 },
      matches: { total: matchesForEvent(events[0].eventId, 200).length, created24h: 12 },
      audit24h: 22
    });
  }),

  http.get(`${BASE}/admin/events`, async () => {
    await delay(120);
    return HttpResponse.json({
      items: events.map((event) => ({
        eventId: event.eventId,
        name: event.eventName,
        venue: event.venue,
        qrCode: event.qrCode,
        expirationCode: event.expirationCode,
        expired: event.expired,
        participantCount: matchesForEvent(event.eventId, 200).length + 1
      })),
      total: events.length,
      page: 0,
      size: 20
    });
  }),

  http.get(`${BASE}/admin/events/:eventId`, async ({ params }) => {
    const event = findEvent(params.eventId as string);
    if (!event) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      eventId: event.eventId,
      name: event.eventName,
      venue: event.venue,
      qrCode: event.qrCode,
      expirationCode: event.expirationCode,
      expired: event.expired,
      participantCount: matchesForEvent(event.eventId, 200).length + 1,
      centroidLat: 13.724,
      centroidLon: 100.56,
      matchCount: matchesForEvent(event.eventId, 200).length,
      messageCount: listThreads().length
    });
  }),

  http.get(`${BASE}/admin/events/:eventId/participants`, async ({ params }) => {
    const eventId = params.eventId as string;
    return HttpResponse.json({
      items: allUsers.slice(0, 8).map((user, index) => ({
        userId: user.userId,
        firstName: user.firstName,
        lastName: user.lastName,
        institution: user.institution,
        lastLat: 13.724 + index * 0.001,
        lastLon: 100.56 + index * 0.001,
        lastUpdate: new Date().toISOString(),
        selectedRadius: 50
      })),
      total: allUsers.length,
      page: 0,
      size: 50
    });
  }),

  http.get(`${BASE}/admin/events/:eventId/heatmap`, async () => {
    return HttpResponse.json(
      allUsers.slice(0, 8).map((_, index) => ({
        lat: 13.724 + index * 0.001,
        lon: 100.56 + index * 0.001,
        lastUpdate: new Date().toISOString()
      }))
    );
  }),

  http.get(`${BASE}/admin/users`, async () => {
    await delay(120);
    return HttpResponse.json({
      items: allUsers.map((user, index) => ({
        userId: user.userId,
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        institution: user.institution,
        role: index === 0 ? "SUPER_ADMIN" : "USER",
        suspended: false,
        deleted: false,
        createdAt: new Date(Date.now() - index * 3600_000).toISOString()
      })),
      total: allUsers.length,
      page: 0,
      size: 25
    });
  }),

  http.get(`${BASE}/admin/users/:userId`, async ({ params }) => {
    const user = findUser(params.userId as string);
    if (!user) return new HttpResponse(null, { status: 404 });
    const now = new Date().toISOString();
    return HttpResponse.json({
      userId: user.userId,
      email: user.email,
      role: user.userId === CURRENT_USER_ID ? "SUPER_ADMIN" : "USER",
      suspended: false,
      deleted: false,
      createdAt: now,
      suspendedAt: null,
      deletedAt: null,
      firstName: user.firstName,
      lastName: user.lastName,
      academicTitle: user.academicTitle,
      institution: user.institution,
      profilePictureUrl: user.profilePictureUrl,
      interests: user.userId === CURRENT_USER_ID
        ? userInterests.map((interest) => ({
            interestId: interest.interestId,
            type: interest.type,
            content: interest.content,
            keywords: interest.extractedKeywords,
            createdAt: interest.createdAt
          }))
        : [],
      events: events.slice(0, 2).map((event) => ({
        eventId: event.eventId,
        name: event.eventName,
        joinedAt: now,
        selectedRadius: 50
      })),
      matchCount: matchesForEvent(events[0].eventId, 200).length,
      chatMessageCount: listThreads().length,
      deviceCount: 1,
      snsLinkCount: snsLinks.length,
      recentAudit: [
        {
          id: "audit-1",
          actorUserId: CURRENT_USER_ID,
          action: "auth.login.success",
          resourceType: "user",
          resourceId: user.userId,
          payload: "{}",
          createdAt: now
        }
      ]
    });
  }),

  http.post(`${BASE}/admin/users/:userId/suspend`, async () => HttpResponse.json({ ok: true })),
  http.post(`${BASE}/admin/users/:userId/unsuspend`, async () => HttpResponse.json({ ok: true })),
  http.post(`${BASE}/admin/users/:userId/role`, async () => HttpResponse.json({ ok: true })),
  http.delete(`${BASE}/admin/users/:userId`, async () => HttpResponse.json({ ok: true })),

  http.get(`${BASE}/admin/audit`, async () => {
    return HttpResponse.json({
      items: [
        {
          id: "audit-1",
          actorUserId: CURRENT_USER_ID,
          action: "auth.login.success",
          resourceType: "user",
          resourceId: CURRENT_USER_ID,
          payload: "{}",
          createdAt: new Date().toISOString()
        }
      ],
      total: 1,
      page: 0,
      size: 50
    });
  }),

  http.get(`${BASE}/admin/ops/outbox`, async () => {
    return HttpResponse.json({
      items: [
        {
          outboxId: "outbox-1",
          userId: CURRENT_USER_ID,
          kind: "chat.message",
          status: "FAILED",
          attempts: 3,
          lastError: "demo failure",
          createdAt: new Date().toISOString(),
          deliveredAt: null
        }
      ],
      total: 1,
      page: 0,
      size: 50
    });
  }),

  http.post(`${BASE}/admin/ops/outbox/:outboxId/retry`, async () => HttpResponse.json({ ok: true })),
  http.post(`${BASE}/admin/dev/reset-demo`, async () => HttpResponse.json({ ok: true }))
];

export const handlersByDomain: Record<MockDomain, HttpHandler[]> = {
  auth: authHandlers,
  admin: adminHandlers,
  profile: profileHandlers,
  sns: snsHandlers,
  interests: interestsHandlers,
  events: eventsHandlers,
  matches: matchesHandlers,
  chat: chatHandlers,
  users: usersHandlers,
  devices: devicesHandlers,
  account: accountHandlers
};

export const handlers = Object.values(handlersByDomain).flat();

function withAttendance(evt: typeof events[number]) {
  // Mirror the backend shape: every joined fellow counts. For NeurIPS the seeder puts every
  // demo user in; ACL is a subset; ICML is expired. matchesForEvent(_,200) returns one row per
  // peer, so +1 accounts for the current user themselves.
  return { ...evt, attendanceCount: matchesForEvent(evt.eventId, 200).length + 1 };
}

function extractKeywordsMock(text: string): string[] {
  const pool = [
    "graph-neural-networks",
    "transformers",
    "attention",
    "federated-learning",
    "privacy",
    "explainability",
    "knowledge-graphs",
    "contrastive-learning",
    "diffusion-models",
    "multi-modal",
    "retrieval-augmented-generation"
  ];
  const lower = text.toLowerCase();
  const hits = pool.filter((k) => lower.includes(k.split("-")[0]));
  if (hits.length >= 3) return hits.slice(0, 5);
  // Seed with some deterministic extras so UI never looks empty.
  return [...hits, "nlp", "machine-learning"].slice(0, 3);
}
