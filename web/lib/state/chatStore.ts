"use client";

import { create } from "zustand";

interface ChatState {
  unread: Record<string, number>; // keyed by `${eventId}:${otherUserId}`
  incrementUnread: (eventId: string, otherUserId: string) => void;
  clearUnread: (eventId: string, otherUserId: string) => void;
  totalUnread: () => number;
}

export const useChatStore = create<ChatState>((set, get) => ({
  unread: {},
  incrementUnread: (eventId, otherUserId) => {
    const key = `${eventId}:${otherUserId}`;
    set((s) => ({ unread: { ...s.unread, [key]: (s.unread[key] ?? 0) + 1 } }));
  },
  clearUnread: (eventId, otherUserId) => {
    const key = `${eventId}:${otherUserId}`;
    set((s) => {
      const next = { ...s.unread };
      delete next[key];
      return { unread: next };
    });
  },
  totalUnread: () => Object.values(get().unread).reduce((a, b) => a + b, 0)
}));
