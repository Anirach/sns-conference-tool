"use client";

import { format } from "date-fns";
import { cn } from "@/lib/utils/cn";
import type { ChatMessage } from "@/lib/fixtures/types";

interface MessageBubbleProps {
  message: ChatMessage;
  isMine: boolean;
  showTime?: boolean;
}

export function MessageBubble({ message, isMine, showTime }: MessageBubbleProps) {
  return (
    <div className="flex flex-col">
      {showTime ? (
        <p className="my-2 text-center text-[10px] tabular-nums text-muted-foreground">
          {format(new Date(message.createdAt), "HH:mm")}
        </p>
      ) : null}
      <div className={cn("flex w-full", isMine ? "justify-end" : "justify-start")}>
        <div
          className={cn(
            "max-w-[78%] break-words px-4 py-2.5 font-serif text-sm leading-relaxed animate-fade-in-up",
            isMine
              ? "rounded-bl-2xl rounded-br-sm rounded-tl-2xl rounded-tr-2xl bg-brand-500 text-background"
              : "rounded-bl-sm rounded-br-2xl rounded-tl-2xl rounded-tr-2xl bg-surface-sunken text-foreground"
          )}
        >
          {message.content}
        </div>
      </div>
    </div>
  );
}
