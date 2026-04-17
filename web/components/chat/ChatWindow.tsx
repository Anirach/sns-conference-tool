"use client";

import { useEffect, useRef } from "react";
import { MessageBubble } from "./MessageBubble";
import { CURRENT_USER_ID, type ChatMessage } from "@/lib/fixtures";

interface ChatWindowProps {
  messages: ChatMessage[];
  typing?: boolean;
  footer?: React.ReactNode;
}

export function ChatWindow({ messages, typing, footer }: ChatWindowProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    requestAnimationFrame(() => {
      ref.current?.scrollTo({ top: ref.current.scrollHeight, behavior: "smooth" });
    });
  }, [messages.length, typing]);

  return (
    <div
      ref={ref}
      role="log"
      aria-live="polite"
      className="flex-1 space-y-2 overflow-y-auto bg-surface-muted px-4 py-2"
      data-selectable
    >
      {messages.map((m, i) => {
        const prev = messages[i - 1];
        const showTime =
          !prev || new Date(m.createdAt).getTime() - new Date(prev.createdAt).getTime() > 5 * 60 * 1000;
        return (
          <MessageBubble
            key={m.messageId}
            message={m}
            isMine={m.fromUserId === CURRENT_USER_ID}
            showTime={showTime}
          />
        );
      })}
      {typing ? <div className="pt-1">{footer}</div> : null}
    </div>
  );
}
