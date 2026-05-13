"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import type { Client } from "@stomp/stompjs";
import { createStompClient } from "./client";
import { mockStomp } from "./mock";
import { chatApi } from "../api/chat";
import { useAuthStore } from "../state/authStore";
import { CURRENT_USER_ID, type ChatMessage, type Match } from "../fixtures";

/** True while frontend-only dev mode keeps chat on the in-memory mock relay. */
function chatMocked(): boolean {
  if (typeof window === "undefined") return false;
  const list = (window as unknown as { __mswDomains?: string[] }).__mswDomains;
  return Array.isArray(list) && list.includes("chat");
}

export function useChat(eventId: string, otherUserId: string) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const accessToken = useAuthStore((s) => s.tokens?.accessToken);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (chatMocked()) {
      mockStomp.activate();
      const unsub = mockStomp.subscribeChat(eventId, (msg) => {
        if (msg.fromUserId === otherUserId || msg.toUserId === otherUserId) {
          setMessages((prev) => [...prev, msg]);
        }
      });
      return unsub;
    }
    if (!accessToken) return;

    // Real STOMP. Backend fans out new messages to both fromUserId AND toUserId on
    // /user/queue/chat (see RedisChatRelay), so the sender's optimistic insert isn't
    // needed — the publish round-trips back through the subscription.
    const client = createStompClient(accessToken);
    clientRef.current = client;
    client.onConnect = () => {
      client.subscribe("/user/queue/chat", (frame) => {
        try {
          const msg = JSON.parse(frame.body) as ChatMessage;
          if (
            msg.eventId === eventId &&
            (msg.fromUserId === otherUserId || msg.toUserId === otherUserId)
          ) {
            setMessages((prev) => {
              // De-dupe if the history fetch + the WS echo both deliver the same row.
              if (prev.some((m) => m.messageId === msg.messageId)) return prev;
              return [...prev, msg];
            });
          }
        } catch {
          // malformed frame — backend writes JSON, so this means a relay bug; drop.
        }
      });
    };
    client.activate();
    return () => {
      void client.deactivate();
      clientRef.current = null;
    };
  }, [eventId, otherUserId, accessToken]);

  const send = useCallback(
    (content: string) => {
      const trimmed = content.trim();
      if (!trimmed) return;
      if (chatMocked()) {
        mockStomp.sendChat({
          messageId: `msg-local-${Date.now()}`,
          eventId,
          fromUserId: CURRENT_USER_ID,
          toUserId: otherUserId,
          content: trimmed,
          readFlag: false,
          createdAt: new Date().toISOString()
        });
        return;
      }
      const clientMessageId =
        typeof crypto !== "undefined" && "randomUUID" in crypto
          ? crypto.randomUUID()
          : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      const client = clientRef.current;
      if (client?.connected) {
        client.publish({
          destination: "/app/chat.send",
          body: JSON.stringify({ eventId, toUserId: otherUserId, content: trimmed, clientMessageId })
        });
        return;
      }

      // Fallback for environments where WebSocket/STOMP is blocked by a proxy or still reconnecting.
      // Persist over REST and optimistically append the saved row so the sender sees it immediately.
      void chatApi
        .send({ eventId, toUserId: otherUserId, content: trimmed, clientMessageId })
        .then((res) => {
          const msg = res.data;
          setMessages((prev) => {
            if (prev.some((m) => m.messageId === msg.messageId)) return prev;
            return [...prev, msg];
          });
        });
    },
    [eventId, otherUserId]
  );

  return { messages, setMessages, send };
}

export function useMatchNotifications(onNewMatch: (m: Match) => void) {
  // Real-backend new-match notifications are pushed through the push outbox, not the
  // WS chat relay. Stays on the mock until Web Push lands.
  useEffect(() => {
    mockStomp.activate();
    return mockStomp.subscribeMatches(onNewMatch);
  }, [onNewMatch]);
}
