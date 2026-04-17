import type { ChatMessage } from "./types";
import { CURRENT_USER_ID } from "./users";

const NEURIPS = "evt-neurips-2026-bkk";

const iso = (minutesAgo: number) =>
  new Date(Date.now() - minutesAgo * 60 * 1000).toISOString();

function thread(
  otherUserId: string,
  lines: Array<{ from: "me" | "them"; text: string; minutesAgo: number }>
): ChatMessage[] {
  return lines.map((l, i) => ({
    messageId: `msg-${otherUserId}-${i}`,
    eventId: NEURIPS,
    fromUserId: l.from === "me" ? CURRENT_USER_ID : otherUserId,
    toUserId: l.from === "me" ? otherUserId : CURRENT_USER_ID,
    content: l.text,
    readFlag: true,
    createdAt: iso(l.minutesAgo)
  }));
}

export const chatHistory: ChatMessage[] = [
  ...thread("u-0002", [
    { from: "them", text: "Hi Alex! Saw you work on GNNs for drug discovery — I gave a talk on that this morning.", minutesAgo: 120 },
    { from: "me", text: "Prof. Smith! I caught part of it online. The attention-pooling result was striking.", minutesAgo: 118 },
    { from: "them", text: "Thank you. Are you around for coffee after session 3?", minutesAgo: 117 },
    { from: "me", text: "Yes, where is good? Main hall?", minutesAgo: 115 },
    { from: "them", text: "Main hall espresso bar, see you at 3:30.", minutesAgo: 114 }
  ]),
  ...thread("u-0003", [
    { from: "me", text: "สวัสดีครับอาจารย์! You shared common keywords with me — federated learning and privacy.", minutesAgo: 75 },
    { from: "them", text: "Hello! Yes, we run a small FL group at Chula. Would love to compare notes.", minutesAgo: 73 },
    { from: "me", text: "Happy to. Are you presenting a poster?", minutesAgo: 70 },
    { from: "them", text: "Thursday afternoon, poster #127.", minutesAgo: 69 }
  ]),
  ...thread("u-0004", [
    { from: "them", text: "Your abstract on heterogeneous graphs was fascinating.", minutesAgo: 50 },
    { from: "me", text: "Thanks Hannah! Have you tried the multi-relational benchmarks from last year?", minutesAgo: 48 },
    { from: "them", text: "Briefly. Do you have comparisons?", minutesAgo: 46 }
  ]),
  ...thread("u-0005", [
    { from: "me", text: "こんにちは Yuki, nice to match!", minutesAgo: 30 },
    { from: "them", text: "Hello! Do you have a minute to chat about long-context models?", minutesAgo: 29 },
    { from: "me", text: "Of course, where are you standing?", minutesAgo: 28 }
  ]),
  ...thread("u-0006", [
    { from: "them", text: "Hi! Matched on transformers + attention. Quick question about your linear-attention variant.", minutesAgo: 15 },
    { from: "me", text: "Happy to — what's the question?", minutesAgo: 14 }
  ]),
  ...thread("u-0009", [
    { from: "them", text: "Bonjour! We share diffusion-models and multi-modal as keywords.", minutesAgo: 8 },
    { from: "me", text: "Bonjour Jean! Yes, I'm curious if INRIA's latest work is open-source?", minutesAgo: 6 }
  ])
];

export function chatForPair(eventId: string, otherUserId: string): ChatMessage[] {
  return chatHistory
    .filter(
      (m) =>
        m.eventId === eventId &&
        (m.fromUserId === otherUserId || m.toUserId === otherUserId)
    )
    .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

export function appendChatMessage(msg: ChatMessage): void {
  chatHistory.push(msg);
}
