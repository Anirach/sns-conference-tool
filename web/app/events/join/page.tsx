"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Calendar, QrCode, MapPin } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { EmptyState } from "@/components/ui/EmptyState";
import { useToast } from "@/components/ui/Toast";
import { eventsApi } from "@/lib/api/events";
import { bridge } from "@/lib/bridge/client";
import type { ConferenceEvent } from "@/lib/fixtures/types";
import type { QrScanResult } from "@/lib/bridge/types";

export default function JoinEventPage() {
  const router = useRouter();
  const { toast } = useToast();
  const [code, setCode] = useState("");

  const { data: joined = [] } = useQuery<ConferenceEvent[]>({
    queryKey: ["events", "joined"],
    queryFn: async () => (await eventsApi.listJoined()).data
  });

  const joinMut = useMutation({
    mutationFn: async (eventCode: string) => (await eventsApi.join({ eventCode })).data,
    onSuccess: (r) => {
      toast({ title: `Joined ${r.event.eventName}`, variant: "success" });
      router.push(`/events/${r.event.eventId}`);
    },
    onError: () => toast({ title: "Could not join", description: "Check the code and try again.", variant: "error" })
  });

  async function onScanQr() {
    try {
      const res = await bridge.call<QrScanResult>("qr.scan");
      if (res.eventCode) joinMut.mutate(res.eventCode);
    } catch {
      toast({ title: "Scanner unavailable", description: "Enter the code manually.", variant: "error" });
    }
  }

  return (
    <AppShell title="Events">
      <div className="flex flex-col gap-4">
        <Card>
          <CardHeader>
            <CardTitle>Join an event</CardTitle>
            <CardDescription>Scan the QR shown at the venue entrance — or type the code.</CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <Button onClick={onScanQr} loading={joinMut.isPending} className="gap-2">
              <QrCode className="h-4 w-4" />
              Scan QR code
            </Button>
            <div className="text-center text-xs text-gray-400">or</div>
            <form
              className="flex gap-2"
              onSubmit={(e) => {
                e.preventDefault();
                if (code.trim()) joinMut.mutate(code.trim().toUpperCase());
              }}
            >
              <Input
                placeholder="e.g. NEURIPS2026"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="uppercase"
              />
              <Button type="submit" disabled={!code.trim()} loading={joinMut.isPending}>
                Join
              </Button>
            </form>
          </CardContent>
        </Card>

        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-500">Your events</h2>
          {joined.length === 0 ? (
            <EmptyState
              icon={Calendar}
              title="No active event"
              description="Join an event by scanning the QR code at the registration desk."
            />
          ) : (
            <div className="flex flex-col gap-2">
              {joined.map((e) => (
                <button
                  key={e.eventId}
                  onClick={() => router.push(`/events/${e.eventId}`)}
                  className="flex items-start gap-3 rounded-2xl border border-gray-200 bg-white p-4 text-left hover:bg-gray-50"
                >
                  <div className="rounded-lg bg-brand-50 p-2 text-brand-600">
                    <Calendar className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-semibold text-gray-900">{e.eventName}</div>
                    <div className="mt-0.5 flex items-center gap-1.5 truncate text-xs text-gray-500">
                      <MapPin className="h-3.5 w-3.5" />
                      {e.venue}
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </section>
      </div>
    </AppShell>
  );
}
