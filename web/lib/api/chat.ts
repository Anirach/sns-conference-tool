import api from "./axios";
import type { ChatMessage, ChatThread } from "../fixtures/types";

export interface ChatHistory {
  messages: ChatMessage[];
}

export const chatApi = {
  threads: () => api.get<ChatThread[]>(`/chats`),
  history: (eventId: string, otherUserId: string, since?: string) =>
    api.get<ChatHistory>(`/chat/${eventId}/${otherUserId}`, { params: since ? { since } : {} }),
  markRead: (messageId: string) => api.post<{ ok: true }>(`/chat/read`, { messageId })
};
