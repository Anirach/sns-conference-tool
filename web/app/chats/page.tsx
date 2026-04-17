"use client";

import { useQuery } from "@tanstack/react-query";
import { formatDistanceToNow } from "date-fns";
import Link from "next/link";
import { MessageCircle } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { UserAvatar } from "@/components/ui/Avatar";
import { OnlineDot } from "@/components/ui/OnlineDot";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { chatApi } from "@/lib/api/chat";
import type { ChatThread } from "@/lib/fixtures/types";

export default function ChatsPage() {
  const { data: threads, isLoading } = useQuery<ChatThread[]>({
    queryKey: ["chats"],
    queryFn: async () => (await chatApi.threads()).data,
    refetchInterval: 15_000
  });

  return (
    <AppShell title="Letters" eyebrow="The Correspondence">
      <div className="flex-1 px-5 pt-6 pb-8">
        <header className="mb-5 hairline-b pb-5">
          <p className="eyebrow text-brass-500">In Correspondence</p>
          <h2 className="mt-2 font-serif text-3xl leading-tight text-foreground">
            Recent <span className="italic">Letters</span>
          </h2>
          <p className="mt-1 font-serif text-xs italic text-muted-foreground">
            Conversations with fellows you have met.
          </p>
        </header>

        {isLoading ? (
          <div>
            {[0, 1, 2].map((i) => (
              <div key={i} className="py-4 hairline-b">
                <Skeleton className="h-12 w-full" />
              </div>
            ))}
          </div>
        ) : !threads || threads.length === 0 ? (
          <EmptyState
            icon={MessageCircle}
            title="No letters yet"
            description="When you and a fellow match, you can begin a correspondence here."
          />
        ) : (
          <div>
            {threads.map((t) => {
              const parts = t.otherName.split(" ").filter(Boolean);
              const first = parts[0] ?? "?";
              const last = parts.slice(1).join(" ") || parts[0] || "?";
              return (
                <Link
                  key={t.threadId}
                  href={`/events/${t.eventId}/chat/${t.otherUserId}`}
                  className="flex items-start gap-3 py-4 hairline-b transition-colors hover:bg-surface-muted"
                >
                  <div className="relative">
                    <UserAvatar firstName={first} lastName={last} src={t.otherPictureUrl} size={48} shape="square" />
                    <OnlineDot online className="absolute -bottom-0.5 -right-0.5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-baseline justify-between gap-2">
                      <p className="truncate font-serif text-base text-foreground">{t.otherName}</p>
                      <span className="eyebrow shrink-0 text-foreground/40 tabular-nums">
                        {formatDistanceToNow(new Date(t.lastMessageAt), { addSuffix: false })}
                      </span>
                    </div>
                    <p className="mt-0.5 truncate font-serif text-xs italic text-muted-foreground">
                      {t.otherInstitution ?? "Independent scholar"}
                    </p>
                    <div className="mt-1.5 flex items-center gap-2">
                      <p className="truncate font-serif text-sm text-foreground/80">
                        {t.lastFromMe ? <span className="text-foreground/40">You: </span> : null}
                        {t.lastMessagePreview}
                      </p>
                      {t.unread > 0 ? (
                        <span className="eyebrow inline-flex h-5 shrink-0 items-center rounded-full bg-brass-500 px-2 text-background tabular-nums">
                          {t.unread}
                        </span>
                      ) : null}
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </AppShell>
  );
}
