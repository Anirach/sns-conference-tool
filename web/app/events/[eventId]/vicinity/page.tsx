"use client";

import { useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { Users } from "lucide-react";
import { useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { MatchCard } from "@/components/match/MatchCard";
import { RadiusSelector } from "@/components/match/VicinityRadiusSelector";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { eventsApi } from "@/lib/api/events";
import { useEventStore } from "@/lib/state/eventStore";

type Sort = "similarity" | "distance" | "recent";

export default function VicinityPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const radius = useEventStore((s) => s.radius);
  const setRadius = useEventStore((s) => s.setRadius);
  const [mutualOnly, setMutualOnly] = useState(false);
  const [sort, setSort] = useState<Sort>("similarity");

  const { data, isLoading } = useQuery({
    queryKey: ["vicinity", eventId, radius],
    queryFn: async () => (await eventsApi.vicinity(eventId, radius)).data,
    refetchInterval: 30_000
  });

  let list = (data?.matches ?? []).slice();
  if (mutualOnly) list = list.filter((m) => m.mutual);
  list.sort((a, b) => {
    if (sort === "similarity") return b.similarity - a.similarity;
    if (sort === "distance") return a.distanceMeters - b.distanceMeters;
    return b.similarity - a.similarity;
  });

  return (
    <AppShell title="Fellows" eyebrow="The Register" showBack>
      <div className="flex-1 px-5 pt-6 pb-8">
        <header className="mb-6 hairline-b pb-5">
          <p className="eyebrow text-brass-500">Vol. IV — Issue II</p>
          <h2 className="mt-2 font-serif text-3xl leading-tight text-foreground">
            Intellectual <span className="italic">Affinities</span>
          </h2>
          <p className="mt-1 font-serif text-sm italic text-muted-foreground">
            Curated by proximity &amp; shared inquiry.
          </p>
        </header>

        <div className="mb-4 flex items-center justify-between hairline-b pb-4">
          <RadiusSelector value={radius} onChange={setRadius} />
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value as Sort)}
            className="eyebrow cursor-pointer bg-transparent text-foreground/60 outline-none"
          >
            <option value="similarity">By Affinity</option>
            <option value="distance">By Proximity</option>
            <option value="recent">By Recency</option>
          </select>
        </div>

        <div className="mb-6 flex flex-wrap items-center gap-x-5 gap-y-2 hairline-b pb-4">
          <button
            type="button"
            onClick={() => setMutualOnly((v) => !v)}
            className={`eyebrow pb-1 transition-colors ${
              mutualOnly ? "border-b border-brass-500 text-brass-500" : "text-foreground/40 hover:text-foreground"
            }`}
          >
            Mutual
          </button>
          <span className="eyebrow text-foreground/30">·</span>
          <button
            type="button"
            onClick={() => setMutualOnly(false)}
            className={`eyebrow pb-1 transition-colors ${
              !mutualOnly ? "border-b border-brass-500 text-brass-500" : "text-foreground/40 hover:text-foreground"
            }`}
          >
            All
          </button>
          <span className="ml-auto eyebrow text-foreground/40 tabular-nums">
            {list.length} within {data?.radius ?? radius}m
          </span>
        </div>

        {isLoading ? (
          <div className="flex flex-col">
            {[0, 1, 2].map((i) => (
              <div key={i} className="py-5 hairline-b">
                <Skeleton className="h-16" />
              </div>
            ))}
          </div>
        ) : list.length === 0 ? (
          <EmptyState
            icon={Users}
            title="No correspondents"
            description="Widen the radius or relax your filters to find more fellows."
          />
        ) : (
          <div className="animate-fade-in-up">
            {list.map((m, i) => (
              <MatchCard key={m.matchId} match={m} eventId={eventId} index={i} />
            ))}
          </div>
        )}
      </div>
    </AppShell>
  );
}
