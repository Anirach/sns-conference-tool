import { Client } from "@stomp/stompjs";

/**
 * Derive the STOMP origin at request time from `window.location`. This keeps
 * the WS handshake same-origin with the page, which means:
 *   - No NEXT_PUBLIC_WS_ORIGIN build-arg needed on prod images.
 *   - Whatever proxy / tunnel terminates https for the page also handles /ws.
 *   - Mixed-content is impossible (https page → wss; http page → ws).
 *
 * Falls back to NEXT_PUBLIC_WS_ORIGIN for SSR / build-time imports only.
 */
function resolveWsOrigin(): string {
  if (typeof window !== "undefined" && window.location?.host) {
    const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${proto}//${window.location.host}`;
  }
  return process.env.NEXT_PUBLIC_WS_ORIGIN ?? "ws://localhost:8080";
}

/**
 * Real STOMP client factory — used in pass 2 once the Spring Boot backend is live.
 * Pass 1 code should use {@link mockStomp} from ./mock.ts via the hooks in ./hooks.ts.
 */
export function createStompClient(jwt: string): Client {
  return new Client({
    brokerURL: `${resolveWsOrigin()}/ws`,
    connectHeaders: { Authorization: `Bearer ${jwt}` },
    reconnectDelay: 3000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
    debug: () => {}
  });
}
