"use client";

import Link from "next/link";
import { MapPin } from "lucide-react";
import { UserAvatar } from "@/components/ui/Avatar";
import { OnlineDot } from "@/components/ui/OnlineDot";
import { SimilarityBar } from "./SimilarityBar";
import type { Match } from "@/lib/fixtures/types";

interface MatchCardProps {
  match: Match;
  /** @deprecated no longer used — MatchCard links to /matches/:matchId directly. Kept for API stability. */
  eventId?: string;
  index?: number;
}

export function MatchCard({ match, index }: MatchCardProps) {
  const pct = Math.round(match.similarity * 100);
  const parts = match.name.split(" ").filter(Boolean);
  const first = parts[0] ?? "";
  const last = parts.slice(1).join(" ") || parts[0] || "?";
  const visible = match.commonKeywords.slice(0, 3);
  const extra = match.commonKeywords.length - visible.length;

  return (
    <Link
      href={`/matches/${match.matchId}`}
      className="group block bg-card px-1 py-5 transition-colors hover:bg-surface-muted hairline-b"
    >
      <article className="grid grid-cols-[64px_1fr_auto] items-start gap-4">
        <div className="relative">
          <UserAvatar firstName={first} lastName={last} src={match.profilePictureUrl} size={64} shape="square" />
          {typeof index === "number" ? (
            <span className="absolute -left-2 -top-2 bg-background px-1 font-serif text-xs italic leading-none text-brass-500">
              {String(index + 1).padStart(2, "0")}
            </span>
          ) : null}
          <OnlineDot online className="absolute -bottom-1 -right-1" />
        </div>

        <div className="min-w-0">
          <p className="eyebrow mb-1 text-brass-500">{match.title ?? "Fellow"}</p>
          <h3 className="truncate font-serif text-lg leading-tight text-foreground transition-colors group-hover:text-brand-500">
            {match.name}
          </h3>
          <p className="mt-0.5 truncate font-serif italic text-xs text-muted-foreground">
            {match.institution ?? "Independent scholar"}
          </p>

          <div className="mt-3 flex flex-wrap items-center gap-x-2 gap-y-1">
            {visible.map((k, i) => (
              <span key={k} className="inline-flex items-center gap-2">
                {i > 0 ? <span className="text-foreground/20">/</span> : null}
                <span className="text-[10px] font-semibold uppercase tracking-[0.14em] text-foreground/60">
                  {k}
                </span>
              </span>
            ))}
            {extra > 0 ? (
              <span className="font-serif text-[10px] italic text-muted-foreground">+{extra}</span>
            ) : null}
          </div>

          <div className="mt-3 flex items-center gap-3 text-[10px] uppercase tracking-[0.14em] text-muted-foreground">
            <span className="inline-flex items-center gap-1 tabular-nums">
              <MapPin className="h-3 w-3" strokeWidth={1.5} /> {match.distanceMeters}m
            </span>
            {match.mutual ? <span className="font-semibold text-brass-600">Mutual</span> : null}
          </div>
        </div>

        <div className="flex flex-col justify-between self-stretch border-l border-foreground/5 pl-2 text-right">
          <div>
            <p className="eyebrow leading-none text-foreground/40">Index</p>
            <p className="mt-1.5 font-serif text-3xl font-light leading-none tabular-nums text-foreground">
              {pct}
              <span className="align-top text-sm">%</span>
            </p>
          </div>
          <div className="ml-auto mt-3 w-12">
            <SimilarityBar value={match.similarity} />
          </div>
        </div>
      </article>
    </Link>
  );
}
