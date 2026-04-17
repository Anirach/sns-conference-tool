import { Client } from "@stomp/stompjs";

const WS_ORIGIN = process.env.NEXT_PUBLIC_WS_ORIGIN ?? "ws://localhost:8080";

/**
 * Real STOMP client factory — used in pass 2 once the Spring Boot backend is live.
 * Pass 1 code should use {@link mockStomp} from ./mock.ts via the hooks in ./hooks.ts.
 */
export function createStompClient(jwt: string): Client {
  return new Client({
    brokerURL: `${WS_ORIGIN}/ws`,
    connectHeaders: { Authorization: `Bearer ${jwt}` },
    reconnectDelay: 3000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
    debug: () => {}
  });
}
