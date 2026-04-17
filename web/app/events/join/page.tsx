"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { MapPin, Clock, Loader2 } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useToast } from "@/components/ui/Toast";
import { QrScannerMock } from "@/components/onboarding/QrScannerMock";
import { eventsApi } from "@/lib/api/events";
import { bridge } from "@/lib/bridge/client";
import { events as allEvents } from "@/lib/fixtures/events";
import type { ConferenceEvent } from "@/lib/fixtures/types";
import type { QrScanResult } from "@/lib/bridge/types";

export default function JoinEventPage() {
  const router = useRouter();
  const { toast } = useToast();
  const [code, setCode] = useState("");
  const [joining, setJoining] = useState<string | null>(null);

  const { data: joined = [] } = useQuery<ConferenceEvent[]>({
    queryKey: ["events", "joined"],
    queryFn: async () => (await eventsApi.listJoined()).data
  });

  const joinMut = useMutation({
    mutationFn: async (eventCode: string) => (await eventsApi.join({ eventCode })).data,
    onSuccess: (r) => {
      toast({ title: `Joined ${r.event.eventName}`, description: "Let's find your fellows.", variant: "success" });
      router.push(`/events/${r.event.eventId}`);
    },
    onError: () => toast({ title: "Cannot join", description: "Check the cipher and try again.", variant: "error" })
  });

  async function onScanQr() {
    try {
      const res = await bridge.call<QrScanResult>("qr.scan");
      if (res.eventCode) joinMut.mutate(res.eventCode);
    } catch {
      toast({ title: "Scanner unavailable", description: "Enter the cipher manually.", variant: "error" });
    }
  }

  return (
    <AppShell title="Scan Session" eyebrow="In Residence" showBack>
      <div className="flex-1 px-5 pt-6 pb-8">
        <QrScannerMock />

        <p className="mt-4 text-center font-serif text-xs italic text-muted-foreground">
          Present the cipher affixed to your event badge.
        </p>

        <div className="mt-6 flex flex-col items-center gap-3">
          <Button onClick={onScanQr} loading={joinMut.isPending} size="lg" fullWidth>
            Scan Cipher
          </Button>
          <div className="eyebrow text-foreground/30">or transcribe</div>
          <form
            className="flex w-full gap-2"
            onSubmit={(e) => {
              e.preventDefault();
              if (code.trim()) joinMut.mutate(code.trim().toUpperCase());
            }}
          >
            <Input
              placeholder="NEURIPS2026"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              className="uppercase tracking-[0.2em]"
            />
            <Button type="submit" disabled={!code.trim()} loading={joinMut.isPending}>
              Enter
            </Button>
          </form>
        </div>

        <section className="mt-10">
          <p className="eyebrow mb-3 text-brass-500 hairline-b pb-2">Demo Sessions</p>
          <div>
            {allEvents.map((e) => {
              const expired = new Date(e.expirationCode) < new Date();
              const already = joined.some((j) => j.eventId === e.eventId);
              return (
                <button
                  key={e.eventId}
                  disabled={expired || joining !== null}
                  onClick={async () => {
                    if (already) {
                      router.push(`/events/${e.eventId}`);
                      return;
                    }
                    setJoining(e.eventId);
                    try {
                      await joinMut.mutateAsync(e.qrCode);
                    } finally {
                      setJoining(null);
                    }
                  }}
                  className="flex w-full items-center justify-between gap-3 py-4 text-left hairline-b transition-colors hover:bg-surface-muted disabled:opacity-50"
                >
                  <div className="min-w-0">
                    <p className="truncate font-serif text-base text-foreground">{e.eventName}</p>
                    <div className="mt-1 flex items-center gap-3 text-[10px] uppercase tracking-[0.14em] text-muted-foreground">
                      <span className="inline-flex items-center gap-1">
                        <MapPin className="h-3 w-3" strokeWidth={1.5} />
                        {e.venue.split(",")[0]}
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <Clock className="h-3 w-3" strokeWidth={1.5} />
                        {expired ? "Adjourned" : "In Session"}
                      </span>
                    </div>
                  </div>
                  {joining === e.eventId ? (
                    <Loader2 className="h-4 w-4 animate-spin text-brass-500" />
                  ) : (
                    <span className="eyebrow text-foreground/40">{already ? "Open" : expired ? "Ended" : "Join"}</span>
                  )}
                </button>
              );
            })}
          </div>
        </section>
      </div>
    </AppShell>
  );
}
