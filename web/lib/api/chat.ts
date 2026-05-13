import api from "./axios";
import type { ChatMessage, ChatThread } from "../fixtures/types";

export interface PeerContext {
  userId: string;
  firstName: string | null;
  lastName: string | null;
  title: string | null;
  institution: string | null;
  pictureUrl: string | null;
  commonKeywords: string[];
}

export interface ChatHistory {
  messages: ChatMessage[];
  peer: PeerContext;
}

export const chatApi = {
  threads: () => api.get<ChatThread[]>(`/chats`),
  history: (eventId: string, otherUserId: string, since?: string) =>
    api.get<ChatHistory>(`/chat/${eventId}/${otherUserId}`, { params: since ? { since } : {} }),
  markRead: (messageId: string) => api.post<{ ok: true }>(`/chat/read`, { messageId })
};
