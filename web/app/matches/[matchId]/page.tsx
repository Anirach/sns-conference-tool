"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { BookOpen, Bookmark, MapPin, MessageCircle, Sparkles } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { UserAvatar } from "@/components/ui/Avatar";
import { OnlineDot } from "@/components/ui/OnlineDot";
import { SimilarityBar } from "@/components/match/SimilarityBar";
import { EmptyState } from "@/components/ui/EmptyState";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/Toast";
import { matchesApi } from "@/lib/api/events";
import { bridge } from "@/lib/bridge/client";
import type { Match } from "@/lib/fixtures/types";

export default function MatchDetailPage() {
  const { matchId } = useParams<{ matchId: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const [saved, setSaved] = useState(false);

  const { data: match, isLoading, isError } = useQuery<Match>({
    queryKey: ["match", matchId],
    queryFn: async () => (await matchesApi.get(matchId)).data
  });

  useEffect(() => {
    bridge
      .call<Array<{ matchId: string }>>("localdb.matches.list", { limit: 1000, offset: 0 })
      .then((items) => setSaved(Array.isArray(items) && items.some((m) => m.matchId === matchId)))
      .catch(() => null);
  }, [matchId]);

  const saveMut = useMutation({
    mutationFn: async () => {
      if (!match) return;
      await bridge.call("localdb.matches.save", { match });
    },
    onSuccess: () => {
      setSaved(true);
      toast({ title: "Saved to your register", variant: "success" });
    }
  });

  if (isLoading) {
    return (
      <AppShell title="Fellow" showBack hideTabs>
        <div className="flex-1 px-5 pt-6 pb-8">
          <Skeleton className="h-40 w-full" />
          <div className="mt-6 space-y-3">
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-4 w-1/2" />
          </div>
        </div>
      </AppShell>
    );
  }

  if (isError || !match) {
    return (
      <AppShell title="Fellow" showBack hideTabs>
        <div className="flex-1 px-5 pt-6 pb-8">
          <EmptyState
            icon={BookOpen}
            title="Fellow not found"
            description="This dossier may have been withdrawn from the register."
            ctaLabel="Back"
            onCta={() => router.back()}
          />
        </div>
      </AppShell>
    );
  }

  const parts = match.name.split(" ").filter(Boolean);
  const first = parts[0] ?? "?";
  const last = parts.slice(1).join(" ") || parts[0] || "?";
  const pct = Math.round(match.similarity * 100);

  return (
    <AppShell title="Dossier" eyebrow="The Register" showBack hideTabs>
      <div className="flex-1 px-5 pt-6 pb-8">
        {/* Hero */}
        <article className="relative hairline-b pb-6">
          <span className="absolute right-0 top-0 text-[10px] font-semibold uppercase tracking-[0.3em] text-foreground/30">
            № {match.matchId.slice(-4)}
          </span>
          <div className="flex items-start gap-5">
            <div className="relative">
              <UserAvatar firstName={first} lastName={last} src={match.profilePictureUrl} size={96} shape="square" />
              <OnlineDot online className="absolute -bottom-1 -right-1" />
            </div>
            <div className="min-w-0 flex-1 pt-1">
              <p className="eyebrow text-brass-500">{match.title ?? "Fellow"}</p>
              <h2 className="mt-1 font-serif text-2xl leading-tight text-foreground">{match.name}</h2>
              <p className="mt-1 font-serif text-sm italic text-muted-foreground">
                {match.institution ?? "Independent scholar"}
              </p>
              <div className="mt-3 flex items-center gap-3 text-[10px] uppercase tracking-[0.14em] text-muted-foreground">
                <span className="inline-flex items-center gap-1 tabular-nums">
                  <MapPin className="h-3 w-3" strokeWidth={1.5} />
                  {match.distanceMeters}m
                </span>
                <span className="text-foreground/30">·</span>
                <span className="text-success font-semibold">In Attendance</span>
                {match.mutual ? (
                  <>
                    <span className="text-foreground/30">·</span>
                    <span className="inline-flex items-center gap-1 font-semibold text-brass-600">
                      <Sparkles className="h-3 w-3" strokeWidth={1.5} />
                      Mutual
                    </span>
                  </>
                ) : null}
              </div>
            </div>
          </div>
        </article>

        {/* Affinity */}
        <section className="py-6 hairline-b">
          <div className="flex items-baseline justify-between">
            <p className="eyebrow text-brass-500">Affinity Index</p>
            <p className="font-serif text-5xl font-light tabular-nums leading-none text-foreground">
              {pct}
              <span className="align-top text-xl">%</span>
            </p>
          </div>
          <div className="mt-4">
            <SimilarityBar value={match.similarity} />
          </div>
          <p className="mt-3 font-serif text-xs italic text-muted-foreground">
            Calculated from shared keywords across your inquiries.
          </p>
        </section>

        {/* Shared keywords */}
        <section className="py-6 hairline-b">
          <p className="eyebrow mb-3 text-brass-500">Shared Inquiries</p>
          <div className="flex flex-wrap gap-x-3 gap-y-2">
            {match.commonKeywords.map((k, i) => (
              <span key={k} className="inline-flex items-center gap-3">
                {i > 0 ? <span className="text-foreground/20">/</span> : null}
                <span className="font-serif text-base text-foreground">{k}</span>
              </span>
            ))}
          </div>
        </section>

        {/* Actions */}
        <div className="mt-6 flex flex-col gap-3">
          <Button
            size="lg"
            fullWidth
            className="gap-2"
            onClick={() => router.push(`/events/${match.eventId}/chat/${match.otherUserId}`)}
          >
            <MessageCircle className="h-4 w-4" strokeWidth={1.5} />
            Open Correspondence
          </Button>
          <Button
            variant="outline"
            size="md"
            fullWidth
            className="gap-2"
            loading={saveMut.isPending}
            disabled={saved}
            onClick={() => saveMut.mutate()}
          >
            <Bookmark className="h-4 w-4" strokeWidth={1.5} />
            {saved ? "Saved to personal register" : "Keep in personal register"}
          </Button>
        </div>

        <p className="mt-10 text-center eyebrow text-foreground/30">
          Registered on {new Date().getFullYear()}
        </p>
      </div>
    </AppShell>
  );
}
