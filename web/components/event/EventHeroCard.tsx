"use client";

import { useEffect, useState } from "react";
import { Clock, MapPin, Users } from "lucide-react";
import { cn } from "@/lib/utils/cn";
import type { ConferenceEvent } from "@/lib/fixtures/types";

interface EventHeroCardProps {
  event: ConferenceEvent;
  attendance?: number;
}

function formatRemaining(ms: number): string {
  if (ms <= 0) return "Ended";
  const h = Math.floor(ms / 3_600_000);
  const m = Math.floor((ms % 3_600_000) / 60_000);
  if (h >= 1) return `${h}h ${m}m`;
  return `${m}m`;
}

export function EventHeroCard({ event, attendance }: EventHeroCardProps) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(id);
  }, []);
  const endsAt = new Date(event.expirationCode).getTime();
  const remaining = endsAt - now;
  const warn = remaining > 0 && remaining < 3_600_000;

  return (
    <article className="relative overflow-hidden bg-brand-500 p-6 text-background">
      <span className="absolute right-3 top-3 text-[10px] font-semibold uppercase tracking-[0.3em] text-background/30">
        № {event.eventId.slice(-2)}
      </span>
      <span className="absolute bottom-3 left-3 right-3 ink-rule bg-background/20" />

      <p className="eyebrow text-brass-300">In Residence</p>
      <h2 className="mt-2 pr-12 font-serif text-2xl leading-tight">{event.eventName}</h2>
      <p className="mt-2 flex items-center gap-1.5 font-serif text-xs italic text-background/75">
        <MapPin className="h-3 w-3" strokeWidth={1.5} /> {event.venue}
      </p>

      <div className="mt-6 flex items-center gap-6 pb-3">
        <div>
          <p className="eyebrow text-background/50">Adjourns</p>
          <p
            className={cn(
              "mt-0.5 inline-flex items-center gap-1.5 font-serif text-lg tabular-nums",
              warn ? "text-brass-300" : "text-background"
            )}
          >
            <Clock className="h-3.5 w-3.5" strokeWidth={1.5} /> {formatRemaining(remaining)}
          </p>
        </div>
        <div>
          <p className="eyebrow text-background/50">In Attendance</p>
          <p className="mt-0.5 inline-flex items-center gap-1.5 font-serif text-lg tabular-nums">
            <Users className="h-3.5 w-3.5" strokeWidth={1.5} /> {attendance ?? 42}
          </p>
        </div>
      </div>
    </article>
  );
}
