"use client";

import { useEffect, useRef } from "react";
import { MessageBubble } from "./MessageBubble";
import { CURRENT_USER_ID, type ChatMessage } from "@/lib/fixtures";

interface ChatWindowProps {
  messages: ChatMessage[];
}

export function ChatWindow({ messages }: ChatWindowProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    ref.current?.scrollTo({ top: ref.current.scrollHeight, behavior: "smooth" });
  }, [messages.length]);

  return (
    <div
      ref={ref}
      role="log"
      aria-live="polite"
      className="flex-1 overflow-y-auto bg-gray-50 px-3 py-4"
    >
      <div className="mx-auto flex max-w-2xl flex-col gap-2">
        {messages.map((m) => (
          <MessageBubble key={m.messageId} message={m} isMine={m.fromUserId === CURRENT_USER_ID} />
        ))}
      </div>
    </div>
  );
}
