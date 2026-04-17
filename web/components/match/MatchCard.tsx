"use client";

import Link from "next/link";
import { MessageSquare, Sparkles } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import { Chip } from "@/components/ui/Chip";
import { Button } from "@/components/ui/Button";
import type { Match } from "@/lib/fixtures/types";
import { cn } from "@/lib/utils/cn";

interface MatchCardProps {
  match: Match;
  eventId: string;
}

export function MatchCard({ match, eventId }: MatchCardProps) {
  const percent = Math.round(match.similarity * 100);
  return (
    <div className="flex gap-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
      <Avatar name={match.name} src={match.profilePictureUrl} size="lg" />

      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h4 className="truncate text-base font-semibold text-gray-900">{match.name}</h4>
              {match.mutual ? (
                <Chip variant="success" className="gap-1">
                  <Sparkles className="h-3 w-3" /> Mutual
                </Chip>
              ) : null}
            </div>
            {match.title || match.institution ? (
              <p className="truncate text-sm text-gray-500">
                {[match.title, match.institution].filter(Boolean).join(" · ")}
              </p>
            ) : null}
          </div>
          <div className="shrink-0 text-right">
            <div className="text-[11px] uppercase tracking-wide text-gray-400">Match</div>
            <div className={cn("text-base font-semibold", percent >= 70 ? "text-brand-600" : "text-gray-700")}>
              {percent}%
            </div>
          </div>
        </div>

        <div className="flex flex-wrap gap-1.5">
          {match.commonKeywords.slice(0, 5).map((k) => (
            <Chip key={k} variant="brand">
              {k}
            </Chip>
          ))}
        </div>

        <div className="flex items-center justify-between pt-1">
          <span className="text-xs text-gray-500">{match.distanceMeters} m away</span>
          <Link href={`/events/${eventId}/chat/${match.otherUserId}`}>
            <Button size="sm" variant="secondary" className="gap-1.5">
              <MessageSquare className="h-4 w-4" /> Chat
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
