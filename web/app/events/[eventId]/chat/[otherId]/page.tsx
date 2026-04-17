"use client";

import { useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { useEffect } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { Avatar } from "@/components/ui/Avatar";
import { ChatWindow } from "@/components/chat/ChatWindow";
import { MessageInput } from "@/components/chat/MessageInput";
import { chatApi } from "@/lib/api/chat";
import { useChat } from "@/lib/ws/hooks";
import { findUser } from "@/lib/fixtures";
import type { ChatHistory } from "@/lib/api/chat";

export default function ChatPage() {
  const { eventId, otherId } = useParams<{ eventId: string; otherId: string }>();

  const { data } = useQuery<ChatHistory>({
    queryKey: ["chat", eventId, otherId],
    queryFn: async () => (await chatApi.history(eventId, otherId)).data
  });

  const { messages, setMessages, send } = useChat(eventId, otherId);

  useEffect(() => {
    if (data?.messages) setMessages(data.messages);
  }, [data, setMessages]);

  const other = findUser(otherId);
  const name = other ? `${other.firstName} ${other.lastName}` : "Unknown";

  return (
    <AppShell
      title={name}
      subtitle={other?.institution ?? undefined}
      showBack
      hideTabs
      right={<Avatar name={name} size="sm" />}
    >
      <div className="-mx-4 -my-4 flex min-h-[calc(100dvh-9rem)] flex-col">
        <ChatWindow messages={messages} />
        <MessageInput onSend={send} />
      </div>
    </AppShell>
  );
}
