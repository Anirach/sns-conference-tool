"use client";

import { useQuery } from "@tanstack/react-query";
import { Heart, Map, MessageCircle, Sparkles, User as UserIcon } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Skeleton } from "@/components/ui/Skeleton";
import { chatApi } from "@/lib/api/chat";
import { eventsApi } from "@/lib/api/events";
import { interestsApi } from "@/lib/api/interests";
import { profileApi } from "@/lib/api/profile";

/**
 * Personal register — a single screen summarising what we know about *you* on this server.
 * Aggregated client-side from existing endpoints; no new backend code.
 */
export default function PersonalRegisterPage() {
  const { data: profile } = useQuery({
    queryKey: ["profile"],
    queryFn: async () => (await profileApi.get()).data
  });
  const { data: interests, isLoading: interestsLoading } = useQuery({
    queryKey: ["interests"],
    queryFn: async () => (await interestsApi.list()).data
  });
  const { data: events, isLoading: eventsLoading } = useQuery({
    queryKey: ["events", "joined"],
    queryFn: async () => (await eventsApi.listJoined()).data
  });
  const { data: threads, isLoading: threadsLoading } = useQuery({
    queryKey: ["chats"],
    queryFn: async () => (await chatApi.threads()).data
  });

  const tiles: { icon: React.ReactNode; label: string; value: string | number; loading?: boolean }[] = [
    {
      icon: <Heart className="h-4 w-4" strokeWidth={1.5} />,
      label: "Interests",
      value: interests?.length ?? "—",
      loading: interestsLoading
    },
    {
      icon: <Map className="h-4 w-4" strokeWidth={1.5} />,
      label: "Sessions joined",
      value: events?.length ?? "—",
      loading: eventsLoading
    },
    {
      icon: <MessageCircle className="h-4 w-4" strokeWidth={1.5} />,
      label: "Correspondences",
      value: threads?.length ?? "—",
      loading: threadsLoading
    },
    {
      icon: <Sparkles className="h-4 w-4" strokeWidth={1.5} />,
      label: "Unread letters",
      value: threads?.reduce((n, t) => n + (t.unread ?? 0), 0) ?? "—",
      loading: threadsLoading
    }
  ];

  return (
    <AppShell title="Register" eyebrow="Personal" showBack>
      <div className="flex-1 space-y-6 px-5 pt-5 pb-12">
        <p className="font-serif text-sm italic text-muted-foreground">
          Everything we hold for you on this server — at a glance. Tap any row to jump to the full view.
        </p>

        <div className="bg-card p-4 hairline">
          <p className="eyebrow text-brass-500">{profile?.academicTitle || "Fellow"}</p>
          <h2 className="mt-1 font-serif text-xl text-foreground">
            {profile ? `${profile.firstName ?? ""} ${profile.lastName ?? ""}`.trim() || "Your profile" : "Loading…"}
          </h2>
          <p className="mt-0.5 font-serif text-sm italic text-muted-foreground">
            {profile?.institution || profile?.email || ""}
          </p>
        </div>

        <div className="grid grid-cols-2 gap-3">
          {tiles.map((t, i) => (
            <div key={i} className="bg-card p-4 hairline">
              <div className="flex items-center gap-2 text-brass-500">
                {t.icon}
                <span className="eyebrow">{t.label}</span>
              </div>
              <div className="mt-3 font-serif text-3xl tabular-nums text-foreground">
                {t.loading ? <Skeleton className="h-8 w-12" /> : t.value}
              </div>
            </div>
          ))}
        </div>

        <div>
          <p className="eyebrow mb-3 text-brass-500">Recent correspondence</p>
          <div className="bg-card hairline">
            {threadsLoading ? (
              <div className="p-4">
                <Skeleton className="h-5" />
              </div>
            ) : (threads ?? []).length === 0 ? (
              <p className="px-4 py-6 font-serif text-sm italic text-muted-foreground">
                No correspondence yet. Match with a fellow to begin.
              </p>
            ) : (
              (threads ?? []).slice(0, 5).map((t, i) => (
                <div
                  key={t.eventId + ":" + t.otherUserId}
                  className={`flex items-center gap-3 px-4 py-3 ${i > 0 ? "hairline-t" : ""}`}
                >
                  <UserIcon className="h-4 w-4 text-brass-500" strokeWidth={1.5} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-serif text-sm text-foreground">{t.otherName}</p>
                    <p className="truncate font-serif text-xs italic text-muted-foreground">
                      {t.lastMessagePreview}
                    </p>
                  </div>
                  {t.unread ? (
                    <span className="eyebrow text-brass-500 tabular-nums">{t.unread}</span>
                  ) : null}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </AppShell>
  );
}
