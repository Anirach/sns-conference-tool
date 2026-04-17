"use client";

import { useQuery } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import { ChevronLeft, X } from "lucide-react";
import { useEffect, useState } from "react";
import { UserAvatar } from "@/components/ui/Avatar";
import { OnlineDot } from "@/components/ui/OnlineDot";
import { ChatWindow } from "@/components/chat/ChatWindow";
import { MessageInput } from "@/components/chat/MessageInput";
import { TypingIndicator } from "@/components/chat/TypingIndicator";
import { chatApi } from "@/lib/api/chat";
import { useChat } from "@/lib/ws/hooks";
import { findUser, findMatchPair } from "@/lib/fixtures";
import type { ChatHistory } from "@/lib/api/chat";

export default function ChatPage() {
  const { eventId, otherId } = useParams<{ eventId: string; otherId: string }>();
  const router = useRouter();

  const { data } = useQuery<ChatHistory>({
    queryKey: ["chat", eventId, otherId],
    queryFn: async () => (await chatApi.history(eventId, otherId)).data
  });

  const { messages, setMessages, send } = useChat(eventId, otherId);
  const [bannerOpen, setBannerOpen] = useState(true);

  useEffect(() => {
    if (data?.messages) setMessages(data.messages);
  }, [data, setMessages]);

  const other = findUser(otherId);
  const match = findMatchPair(eventId, otherId);
  const first = other?.firstName ?? "?";
  const last = other?.lastName ?? "?";

  return (
    <div className="mobile-frame flex h-[100dvh] flex-col">
      <header className="sticky top-0 z-30 bg-background/90 backdrop-blur hairline-b">
        <div className="flex h-14 items-center gap-3 px-3">
          <button
            type="button"
            onClick={() => router.back()}
            className="p-2 text-foreground/70 hover:text-brass-500"
            aria-label="Back"
          >
            <ChevronLeft className="h-5 w-5" strokeWidth={1.5} />
          </button>
          <div className="relative">
            <UserAvatar firstName={first} lastName={last} size={36} shape="square" />
            <OnlineDot online className="absolute -bottom-0.5 -right-0.5" />
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate font-serif text-sm leading-tight text-foreground">
              {first} {last}
            </p>
            <p className="truncate font-serif text-[10px] italic text-muted-foreground">
              {other?.institution ?? "In Residence"}
            </p>
          </div>
        </div>
      </header>

      {bannerOpen && match && match.commonKeywords.length > 0 ? (
        <div className="mx-4 mt-3 flex items-start gap-2 bg-accent px-3 py-2 text-xs text-accent-foreground hairline">
          <span className="flex-1 font-serif italic">
            You matched on: <strong className="font-semibold not-italic">{match.commonKeywords.slice(0, 3).join(", ")}</strong>
          </span>
          <button
            type="button"
            onClick={() => setBannerOpen(false)}
            aria-label="Dismiss"
            className="text-accent-foreground/60 hover:text-accent-foreground"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      ) : null}

      <ChatWindow messages={messages} typing={false} footer={<TypingIndicator />} />
      <MessageInput onSend={send} />
    </div>
  );
}
