"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import { useEffect } from "react";
import Link from "next/link";
import { ChevronRight, MessageCircle, MapPin } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { EventHeroCard } from "@/components/event/EventHeroCard";
import { RadiusSelector } from "@/components/match/VicinityRadiusSelector";
import { MatchCard } from "@/components/match/MatchCard";
import { UserAvatar } from "@/components/ui/Avatar";
import { OnlineDot } from "@/components/ui/OnlineDot";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/Toast";
import { eventsApi } from "@/lib/api/events";
import { useEventStore } from "@/lib/state/eventStore";
import type { ConferenceEvent, Match } from "@/lib/fixtures/types";

export default function EventHomePage() {
  const { eventId } = useParams<{ eventId: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const radius = useEventStore((s) => s.radius);
  const setRadius = useEventStore((s) => s.setRadius);
  const setActiveEvent = useEventStore((s) => s.setActiveEvent);

  const { data: evt } = useQuery<ConferenceEvent>({
    queryKey: ["event", eventId],
    queryFn: async () => (await eventsApi.get(eventId)).data
  });

  const { data: vicinity } = useQuery({
    queryKey: ["vicinity", eventId, radius],
    queryFn: async () => (await eventsApi.vicinity(eventId, radius)).data
  });

  useEffect(() => {
    if (evt) setActiveEvent(evt);
  }, [evt, setActiveEvent]);

  const setRadiusMut = useMutation({
    mutationFn: (r: 20 | 50 | 100) => eventsApi.setRadius(eventId, r)
  });

  const leaveMut = useMutation({
    mutationFn: () => eventsApi.leave(eventId),
    onSuccess: () => {
      toast({ title: "Session adjourned" });
      router.push("/events/join");
    }
  });

  const matches = vicinity?.matches ?? null;
  const mutualTop = matches?.filter((m) => m.mutual).slice(0, 3) ?? [];
  const nearby = matches?.slice(0, 6) ?? [];

  return (
    <AppShell title="Session" eyebrow="In Residence" showBack>
      <div className="relative flex-1 space-y-6 px-5 pt-5 pb-24">
        {evt ? (
          <EventHeroCard event={evt} attendance={matches?.length ?? undefined} />
        ) : (
          <Skeleton className="h-44" />
        )}

        <div className="hairline-b pb-4">
          <p className="eyebrow mb-3 text-brass-500">Proximity</p>
          <RadiusSelector
            value={radius}
            onChange={(r) => {
              setRadius(r);
              setRadiusMut.mutate(r);
            }}
          />
        </div>

        <section>
          <div className="mb-2 flex items-end justify-between hairline-b pb-2">
            <div>
              <p className="eyebrow text-brass-500">Today&apos;s Selection</p>
              <h2 className="mt-1 font-serif text-xl text-foreground">
                Foremost <span className="italic">Affinities</span>
              </h2>
            </div>
            <Link
              href={`/events/${eventId}/vicinity`}
              className="eyebrow inline-flex items-center gap-1 text-foreground/50 hover:text-brass-500"
            >
              See All <ChevronRight className="h-3 w-3" strokeWidth={1.5} />
            </Link>
          </div>
          <div>
            {matches === null
              ? Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="py-5 hairline-b">
                    <Skeleton className="h-16" />
                  </div>
                ))
              : mutualTop.length > 0
              ? mutualTop.map((m, i) => (
                  <MatchCard key={m.matchId} match={m} eventId={eventId} index={i} />
                ))
              : (matches ?? [])
                  .slice(0, 3)
                  .map((m, i) => <MatchCard key={m.matchId} match={m} eventId={eventId} index={i} />)}
          </div>
        </section>

        <section>
          <div className="mb-2 flex items-end justify-between hairline-b pb-2">
            <div>
              <p className="eyebrow text-brass-500">In Proximity</p>
              <h2 className="mt-1 font-serif text-xl text-foreground">
                Nearby <span className="italic">Fellows</span>
              </h2>
            </div>
          </div>
          <div>
            {nearby.map((m) => {
              const parts = m.name.split(" ").filter(Boolean);
              return (
                <Link
                  key={m.matchId}
                  href={`/events/${eventId}/chat/${m.otherUserId}`}
                  className="flex items-center gap-3 py-3 hairline-b hover:bg-surface-muted"
                >
                  <div className="relative">
                    <UserAvatar
                      firstName={parts[0] ?? "?"}
                      lastName={parts.slice(1).join(" ") || parts[0] || "?"}
                      size={40}
                      shape="square"
                    />
                    <OnlineDot online className="absolute -bottom-0.5 -right-0.5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-serif text-sm text-foreground">{m.name}</p>
                    <p className="truncate font-serif text-xs italic text-muted-foreground">{m.institution}</p>
                  </div>
                  <span className="eyebrow inline-flex items-center gap-1 text-foreground/40 tabular-nums">
                    <MapPin className="h-3 w-3" strokeWidth={1.5} />
                    {m.distanceMeters}m
                  </span>
                </Link>
              );
            })}
          </div>
        </section>

        <Button
          variant="ghost"
          size="sm"
          onClick={() => leaveMut.mutate()}
          loading={leaveMut.isPending}
          className="mx-auto mt-4 block text-danger hover:text-danger/80"
        >
          Adjourn session
        </Button>

        <button
          type="button"
          onClick={() => router.push(`/events/${eventId}/vicinity`)}
          className="fixed bottom-24 grid h-12 w-12 place-items-center bg-brand-500 text-background shadow-lg transition-colors hover:bg-brass-500"
          aria-label="Open correspondence"
          style={{ right: "max(1.25rem, calc(50vw - 215px + 1.25rem))" }}
        >
          <MessageCircle className="h-5 w-5" strokeWidth={1.5} />
        </button>
      </div>
    </AppShell>
  );
}
