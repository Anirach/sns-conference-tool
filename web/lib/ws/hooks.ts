"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { mockStomp } from "./mock";
import { CURRENT_USER_ID, type ChatMessage, type Match } from "../fixtures";

export function useChat(eventId: string, otherUserId: string) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const ready = useRef(false);

  useEffect(() => {
    mockStomp.activate();
    const unsub = mockStomp.subscribeChat(eventId, (msg) => {
      if (msg.fromUserId === otherUserId || msg.toUserId === otherUserId) {
        setMessages((prev) => [...prev, msg]);
      }
    });
    ready.current = true;
    return () => {
      unsub();
    };
  }, [eventId, otherUserId]);

  const send = useCallback(
    (content: string) => {
      const trimmed = content.trim();
      if (!trimmed) return;
      mockStomp.sendChat({
        messageId: `msg-local-${Date.now()}`,
        eventId,
        fromUserId: CURRENT_USER_ID,
        toUserId: otherUserId,
        content: trimmed,
        readFlag: false,
        createdAt: new Date().toISOString()
      });
    },
    [eventId, otherUserId]
  );

  return { messages, setMessages, send };
}

export function useMatchNotifications(onNewMatch: (m: Match) => void) {
  useEffect(() => {
    mockStomp.activate();
    return mockStomp.subscribeMatches(onNewMatch);
  }, [onNewMatch]);
}
