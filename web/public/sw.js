// SNS PWA service worker — minimal app-shell cache.
// Strategy:
//   - Pre-cache the start URL + manifest + icons on install.
//   - Stale-while-revalidate for /_next/static/* (long-lived hashed chunks).
//   - Network-only for /api/* and /ws/* (auth-bearing, websocket — never cache).
//   - Network-first with stale fallback for everything else (HTML pages).

const VERSION = "v1";
const SHELL_CACHE = `sns-shell-${VERSION}`;
const SHELL_ASSETS = [
  "/",
  "/manifest.webmanifest",
  "/icons/icon-192.png",
  "/icons/icon-512.png",
  "/icons/apple-touch-icon-180.png"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(SHELL_CACHE).then((cache) =>
      Promise.allSettled(SHELL_ASSETS.map((url) => cache.add(url).catch(() => null)))
    )
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((k) => k.startsWith("sns-shell-") && k !== SHELL_CACHE)
          .map((k) => caches.delete(k))
      )
    )
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // Never cache auth-bearing or realtime traffic.
  if (url.pathname.startsWith("/api/") || url.pathname.startsWith("/ws/")) {
    return; // default network handling
  }

  // Stale-while-revalidate for hashed Next chunks.
  if (url.pathname.startsWith("/_next/static/")) {
    event.respondWith(
      caches.open(SHELL_CACHE).then(async (cache) => {
        const cached = await cache.match(req);
        const network = fetch(req)
          .then((res) => {
            if (res.ok) cache.put(req, res.clone()).catch(() => null);
            return res;
          })
          .catch(() => cached);
        return cached || network;
      })
    );
    return;
  }

  // App shell: network-first, fall back to cached '/' for navigation requests offline.
  if (req.mode === "navigate") {
    event.respondWith(
      fetch(req).catch(() =>
        caches.open(SHELL_CACHE).then((c) => c.match("/") || c.match(req))
      )
    );
    return;
  }

  // Other GET (icons, manifest): cache-first with network refresh.
  event.respondWith(
    caches.open(SHELL_CACHE).then(async (cache) => {
      const cached = await cache.match(req);
      const network = fetch(req)
        .then((res) => {
          if (res.ok) cache.put(req, res.clone()).catch(() => null);
          return res;
        })
        .catch(() => cached);
      return cached || network;
    })
  );
});
