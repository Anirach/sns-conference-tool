"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { MapPin, Users, LogOut } from "lucide-react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/Card";
import { VicinityRadiusSelector } from "@/components/match/VicinityRadiusSelector";
import { useToast } from "@/components/ui/Toast";
import { eventsApi } from "@/lib/api/events";
import { useEventStore } from "@/lib/state/eventStore";
import type { ConferenceEvent } from "@/lib/fixtures/types";
import { useEffect } from "react";

export default function EventHomePage() {
  const { eventId } = useParams<{ eventId: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const { toast } = useToast();
  const radius = useEventStore((s) => s.radius);
  const setRadius = useEventStore((s) => s.setRadius);
  const setActiveEvent = useEventStore((s) => s.setActiveEvent);

  const { data: evt, isLoading } = useQuery<ConferenceEvent>({
    queryKey: ["event", eventId],
    queryFn: async () => (await eventsApi.get(eventId)).data
  });

  useEffect(() => {
    if (evt) setActiveEvent(evt);
  }, [evt, setActiveEvent]);

  const setRadiusMut = useMutation({
    mutationFn: (r: number) => eventsApi.setRadius(eventId, r)
  });

  const leaveMut = useMutation({
    mutationFn: () => eventsApi.leave(eventId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["events", "joined"] });
      toast({ title: "Left event" });
      router.push("/events/join");
    }
  });

  if (isLoading || !evt) {
    return (
      <AppShell title="Event" showBack>
        <div className="py-16 text-center text-sm text-gray-500">Loading…</div>
      </AppShell>
    );
  }

  return (
    <AppShell title={evt.eventName} subtitle={evt.venue} showBack>
      <div className="flex flex-col gap-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <MapPin className="h-4 w-4 text-brand-600" />
              {evt.venue}
            </CardTitle>
            <CardDescription>Active until {new Date(evt.expirationCode).toLocaleString()}</CardDescription>
          </CardHeader>
          <CardContent>
            <VicinityRadiusSelector
              value={radius}
              onChange={(r) => {
                setRadius(r);
                setRadiusMut.mutate(r);
              }}
            />
          </CardContent>
        </Card>

        <Link href={`/events/${eventId}/vicinity`}>
          <Button size="lg" fullWidth className="gap-2">
            <Users className="h-4 w-4" />
            View vicinity ({radius} m)
          </Button>
        </Link>

        <Button
          variant="secondary"
          onClick={() => leaveMut.mutate()}
          loading={leaveMut.isPending}
          className="gap-2 text-red-700 hover:text-red-800"
        >
          <LogOut className="h-4 w-4" />
          Leave event
        </Button>
      </div>
    </AppShell>
  );
}
