"use client";

import { useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { Users } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { MatchCard } from "@/components/match/MatchCard";
import { VicinityRadiusSelector } from "@/components/match/VicinityRadiusSelector";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { eventsApi } from "@/lib/api/events";
import { useEventStore } from "@/lib/state/eventStore";
import type { VicinityResponse } from "@/lib/api/events";

export default function VicinityPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const radius = useEventStore((s) => s.radius);
  const setRadius = useEventStore((s) => s.setRadius);

  const { data, isLoading } = useQuery<VicinityResponse>({
    queryKey: ["vicinity", eventId, radius],
    queryFn: async () => (await eventsApi.vicinity(eventId, radius)).data,
    refetchInterval: 30_000
  });

  return (
    <AppShell title="Nearby matches" showBack>
      <div className="flex flex-col gap-4">
        <div className="rounded-2xl border border-gray-200 bg-white p-4">
          <VicinityRadiusSelector value={radius} onChange={setRadius} />
        </div>

        {isLoading ? (
          <div className="flex flex-col gap-3">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-32 w-full" />
            ))}
          </div>
        ) : data && data.matches.length > 0 ? (
          <>
            <div className="text-xs text-gray-500">
              {data.matches.length} matches within {data.radius} m
            </div>
            <div className="flex flex-col gap-3">
              {data.matches.map((m) => (
                <MatchCard key={m.matchId} match={m} eventId={eventId} />
              ))}
            </div>
          </>
        ) : (
          <EmptyState
            icon={Users}
            title="No matches yet"
            description="Try widening the radius, or add more interests so we can find better overlaps."
          />
        )}
      </div>
    </AppShell>
  );
}
