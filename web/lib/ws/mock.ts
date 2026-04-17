import { appendChatMessage, CURRENT_USER_ID, findUser, type ChatMessage } from "../fixtures";
import type { ChatListener, MatchListener } from "./types";

const DEMO_REPLIES = [
  "Interesting point — do you have a reference?",
  "Agreed. Where are you presenting?",
  "Let's grab coffee after the next session.",
  "I'll send you the preprint link.",
  "That's exactly what we ran into too.",
  "Do you have code available?"
];

class MockStompClient {
  private chatListeners: Map<string, Set<ChatListener>> = new Map();
  private matchListeners: Set<MatchListener> = new Set();
  private intervals: ReturnType<typeof setInterval>[] = [];
  private activated = false;

  activate() {
    if (this.activated) return;
    this.activated = true;
    const timer = setInterval(() => this.emitFakeIncoming(), 12000);
    this.intervals.push(timer);
  }

  deactivate() {
    this.activated = false;
    this.intervals.forEach(clearInterval);
    this.intervals = [];
    this.chatListeners.clear();
    this.matchListeners.clear();
  }

  subscribeChat(eventId: string, listener: ChatListener): () => void {
    const key = `chat:${eventId}`;
    let set = this.chatListeners.get(key);
    if (!set) {
      set = new Set();
      this.chatListeners.set(key, set);
    }
    set.add(listener);
    return () => set!.delete(listener);
  }

  subscribeMatches(listener: MatchListener): () => void {
    this.matchListeners.add(listener);
    return () => this.matchListeners.delete(listener);
  }

  sendChat(msg: ChatMessage): void {
    appendChatMessage(msg);
    const key = `chat:${msg.eventId}`;
    this.chatListeners.get(key)?.forEach((l) => l(msg));
  }

  private emitFakeIncoming(): void {
    const keys = Array.from(this.chatListeners.keys());
    if (!keys.length) return;
    const key = keys[Math.floor(Math.random() * keys.length)];
    const eventId = key.slice("chat:".length);
    const fromUserId = pickRandomOther();
    const reply = DEMO_REPLIES[Math.floor(Math.random() * DEMO_REPLIES.length)];
    const msg: ChatMessage = {
      messageId: `msg-incoming-${Date.now()}`,
      eventId,
      fromUserId,
      toUserId: CURRENT_USER_ID,
      content: reply,
      readFlag: false,
      createdAt: new Date().toISOString()
    };
    appendChatMessage(msg);
    this.chatListeners.get(key)?.forEach((l) => l(msg));
  }
}

function pickRandomOther(): string {
  const pool = ["u-0002", "u-0003", "u-0004", "u-0005", "u-0006", "u-0009"];
  return pool[Math.floor(Math.random() * pool.length)];
}

export const mockStomp = new MockStompClient();

export function ensureUserExists(id: string) {
  return findUser(id);
}
