import { http, HttpResponse, delay, type HttpHandler } from "msw";
import {
  CURRENT_USER_ID,
  allUsers,
  currentUser,
  events,
  eventsByCode,
  findEvent,
  findMatchById,
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
  | "profile"
  | "sns"
  | "interests"
  | "events"
  | "matches"
  | "chat"
  | "users"
  | "devices";

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
  })
];

export const snsHandlers = [
  http.get(`${BASE}/sns`, async () => {
    await delay(100);
    return HttpResponse.json(snsLinks);
  }),

  http.post(`${BASE}/sns/link`, async ({ request }) => {
    const body = (await request.json()) as { provider: "FACEBOOK" | "LINKEDIN"; providerUserId: string };
    const existing = snsLinks.findIndex((l) => l.provider === body.provider);
    const link: SnsLink = {
      provider: body.provider,
      providerUserId: body.providerUserId,
      linkedAt: new Date().toISOString()
    };
    if (existing >= 0) snsLinks[existing] = link;
    else snsLinks.push(link);
    await delay(250);
    return HttpResponse.json(link);
  }),

  http.delete(`${BASE}/sns/:provider`, async ({ params }) => {
    const idx = snsLinks.findIndex((l) => l.provider === params.provider);
    if (idx >= 0) snsLinks.splice(idx, 1);
    await delay(150);
    return HttpResponse.json({ ok: true });
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
    return HttpResponse.json(events.filter((e) => joinedEventIds.has(e.eventId)));
  }),

  http.get(`${BASE}/events/:eventId`, async ({ params }) => {
    const evt = findEvent(params.eventId as string);
    if (!evt) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json(evt);
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
    return HttpResponse.json({ event: evt, joinedAt: new Date().toISOString() });
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
    const messages = chatForPair(params.eventId as string, params.otherUserId as string);
    await delay(150);
    return HttpResponse.json({ messages });
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

export const handlersByDomain: Record<MockDomain, HttpHandler[]> = {
  auth: authHandlers,
  profile: profileHandlers,
  sns: snsHandlers,
  interests: interestsHandlers,
  events: eventsHandlers,
  matches: matchesHandlers,
  chat: chatHandlers,
  users: usersHandlers,
  devices: devicesHandlers
};

export const handlers = Object.values(handlersByDomain).flat();

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
