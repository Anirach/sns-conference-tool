"use client";

import { format } from "date-fns";
import { cn } from "@/lib/utils/cn";
import type { ChatMessage } from "@/lib/fixtures/types";

interface MessageBubbleProps {
  message: ChatMessage;
  isMine: boolean;
}

export function MessageBubble({ message, isMine }: MessageBubbleProps) {
  return (
    <div className={cn("flex w-full", isMine ? "justify-end" : "justify-start")}>
      <div
        className={cn(
          "max-w-[75%] rounded-2xl px-3.5 py-2.5 text-sm shadow-sm",
          isMine
            ? "rounded-br-md bg-brand-600 text-white"
            : "rounded-bl-md bg-white text-gray-900 ring-1 ring-gray-200"
        )}
      >
        <p className="whitespace-pre-wrap break-words">{message.content}</p>
        <div className={cn("mt-1 text-right text-[10px]", isMine ? "text-brand-100" : "text-gray-400")}>
          {format(new Date(message.createdAt), "HH:mm")}
        </div>
      </div>
    </div>
  );
}
