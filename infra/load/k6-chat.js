// k6 load scenario for STOMP chat over WebSocket.
//
// Opens 1000 concurrent WS sessions, each sending 10 msg/s for 2 minutes. Asserts p95 ≤ 200 ms.
// Usage:
//   BASE_WS=ws://localhost:8080/ws JWT=... EVENT_ID=... PEER_USER_ID=... k6 run infra/load/k6-chat.js

import ws from "k6/ws";
import { check } from "k6";
import { Counter, Trend } from "k6/metrics";

export const options = {
  scenarios: {
    chat: {
      executor: "constant-vus",
      vus: 1000,
      duration: "2m30s",
      gracefulStop: "30s",
    },
  },
  thresholds: {
    "chat_roundtrip": ["p(95)<200"],
    "chat_errors": ["count<50"],
  },
};

const BASE_WS = __ENV.BASE_WS || "ws://localhost:8080/ws";
const JWT = __ENV.JWT || "";
const EVENT_ID = __ENV.EVENT_ID || "";
const PEER_USER_ID = __ENV.PEER_USER_ID || "";

if (!JWT || !EVENT_ID || !PEER_USER_ID) {
  throw new Error("Set JWT, EVENT_ID, PEER_USER_ID before running k6-chat.");
}

const roundtrip = new Trend("chat_roundtrip", true);
const errors = new Counter("chat_errors");

export default function () {
  const res = ws.connect(BASE_WS, {}, function (socket) {
    socket.on("open", () => {
      socket.send(stomp("CONNECT", {
        "accept-version": "1.2",
        "host": "sns",
        "Authorization": `Bearer ${JWT}`,
        "heart-beat": "10000,10000",
      }));
      socket.send(stomp("SUBSCRIBE", { id: "sub-0", destination: "/user/queue/chat" }));

      let i = 0;
      socket.setInterval(() => {
        const clientMessageId = `k6-${__VU}-${i++}`;
        const sentAt = Date.now();
        socket.send(stomp("SEND", {
          destination: "/app/chat.send",
          "content-type": "application/json",
        }, JSON.stringify({
          eventId: EVENT_ID,
          toUserId: PEER_USER_ID,
          content: "load-test",
          clientMessageId,
        })));
        socket._sendingAt = sentAt;
      }, 100); // 10 msg/s
    });

    socket.on("message", (raw) => {
      if (raw.startsWith("MESSAGE")) {
        roundtrip.add(Date.now() - (socket._sendingAt || Date.now()));
      } else if (raw.startsWith("ERROR")) {
        errors.add(1);
      }
    });

    socket.on("close", () => {});
    socket.on("error", () => errors.add(1));
    socket.setTimeout(() => socket.close(), 2 * 60 * 1000);
  });
  check(res, { "ws status 101": (r) => r && r.status === 101 });
}

function stomp(cmd, headers, body) {
  let h = "";
  for (const k in headers) h += `${k}:${headers[k]}\n`;
  return `${cmd}\n${h}\n${body || ""}\0`;
}
