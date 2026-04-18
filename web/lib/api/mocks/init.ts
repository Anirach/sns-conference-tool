import { handlersByDomain, type MockDomain } from "./handlers";

const ALL_DOMAINS = Object.keys(handlersByDomain) as MockDomain[];

/**
 * Parses `NEXT_PUBLIC_MOCK_API`. Supported forms:
 *   unset | "0"       → mocks disabled
 *   "1"               → all domains mocked (backward-compatible default)
 *   "auth,profile"    → only listed domains mocked; real backend serves the rest
 *   "1,-auth,-events" → all domains except auth and events (use for Phase cutover)
 */
export function resolveMockDomains(): MockDomain[] {
  const raw = process.env.NEXT_PUBLIC_MOCK_API;
  if (!raw || raw === "0") return [];
  const parts = raw.split(",").map((s) => s.trim()).filter(Boolean);

  let selected: Set<MockDomain>;
  if (parts.includes("1")) {
    selected = new Set(ALL_DOMAINS);
  } else {
    selected = new Set(parts.filter((p) => !p.startsWith("-")) as MockDomain[]);
  }
  for (const p of parts) {
    if (p.startsWith("-")) selected.delete(p.slice(1) as MockDomain);
  }
  return [...selected];
}

export async function initMockApi(): Promise<void> {
  if (typeof window === "undefined") return;
  const domains = resolveMockDomains();

  // If mocks are disabled but an MSW service worker from a previous session is still
  // registered, it'll sit between the browser and the network and mangle requests
  // (observed symptom: /api/auth/login returns 401 in the browser while curl works fine).
  // Proactively unregister every /mockServiceWorker.js so the page self-heals on the
  // next reload without requiring the user to clear site data.
  if (domains.length === 0) {
    if ("serviceWorker" in navigator) {
      const regs = await navigator.serviceWorker.getRegistrations().catch(() => []);
      for (const r of regs) {
        const url = r.active?.scriptURL || r.installing?.scriptURL || r.waiting?.scriptURL || "";
        if (url.includes("mockServiceWorker")) await r.unregister().catch(() => null);
      }
    }
    return;
  }

  if ((window as unknown as { __mswReady?: boolean }).__mswReady) return;

  const { buildWorker } = await import("./browser");
  const worker = buildWorker(domains);
  await worker.start({
    serviceWorker: { url: "/mockServiceWorker.js" },
    onUnhandledRequest: "bypass",
    quiet: true
  });
  (window as unknown as { __mswReady?: boolean; __mswDomains?: MockDomain[] }).__mswReady = true;
  (window as unknown as { __mswDomains?: MockDomain[] }).__mswDomains = domains;
}
