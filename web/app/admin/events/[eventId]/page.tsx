"use client";

import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Trash2 } from "lucide-react";
import { adminApi } from "@/lib/api/admin";
import { AppShell } from "@/components/layout/AppShell";
import { StatCard } from "@/components/admin/StatCard";
import { VenueHeatmap } from "@/components/admin/VenueHeatmap";
import { AdminTable, type ColumnDef } from "@/components/admin/Table";
import { Button } from "@/components/ui/Button";

export default function AdminEventDetailPage() {
  const { eventId } = useParams<{ eventId: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const [confirmDelete, setConfirmDelete] = useState(false);

  const detailQ = useQuery({
    queryKey: ["admin", "event", eventId],
    queryFn: async () => (await adminApi.events.get(eventId)).data,
    refetchInterval: 30_000
  });
  const heatQ = useQuery({
    queryKey: ["admin", "event", eventId, "heatmap"],
    queryFn: async () => (await adminApi.events.heatmap(eventId)).data,
    refetchInterval: 30_000
  });
  const partQ = useQuery({
    queryKey: ["admin", "event", eventId, "participants"],
    queryFn: async () => (await adminApi.events.participants(eventId, 0, 50)).data
  });

  const deleteEvent = useMutation({
    mutationFn: () => adminApi.events.delete(eventId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin", "events"] });
      router.push("/admin/events");
    }
  });

  const cols: ColumnDef<NonNullable<typeof partQ.data>["items"][number]>[] = [
    {
      key: "name",
      header: "Fellow",
      cell: (r) => (
        <div>
          <p className="text-sm">
            {r.firstName ?? "—"} {r.lastName ?? ""}
          </p>
          <p className="mt-0.5 text-xs text-foreground/60">{r.institution ?? "—"}</p>
        </div>
      )
    },
    {
      key: "update",
      header: "Last fix",
      cell: (r) =>
        r.lastUpdate ? (
          <span className="text-xs tabular-nums">{new Date(r.lastUpdate).toLocaleTimeString()}</span>
        ) : (
          <span className="text-xs text-foreground/40">—</span>
        )
    },
    { key: "radius", header: "Radius", align: "right", cell: (r) => `${r.selectedRadius} m` }
  ];

  const detail = detailQ.data;

  return (
    <AppShell title="Session" eyebrow="The Registry" showBack>
      <div className="flex-1 px-5 pt-6 pb-8">
        <header className="mb-5 hairline-b pb-5">
          <p className="eyebrow text-brass-500">Session</p>
          <h2 className="mt-2 font-serif text-3xl leading-tight">
            {detail ? <span className="italic">{detail.name}</span> : <span className="text-foreground/30">…</span>}
          </h2>
          {detail ? (
            <p className="mt-1 font-serif text-sm italic text-muted-foreground">
              {detail.venue} · cipher <code className="not-italic">{detail.qrCode}</code>
            </p>
          ) : null}
        </header>

        <section className="mb-6 grid grid-cols-2 gap-3">
          <StatCard label="In residence" value={detail?.participantCount ?? 0} />
          <StatCard label="Affinities" value={detail?.matchCount ?? 0} />
          <StatCard label="Correspondence" value={detail?.messageCount ?? 0} />
          <StatCard
            label="Status"
            value={detail?.expired ? "Adjourned" : "Active"}
            accent={detail?.expired ? "warn" : "default"}
          />
        </section>

        <section className="mb-6">
          <h3 className="eyebrow mb-3 text-brass-500">Venue heatmap</h3>
          <VenueHeatmap
            points={heatQ.data ?? []}
            centroidLat={detail?.centroidLat}
            centroidLon={detail?.centroidLon}
            size={300}
          />
        </section>

        <section className="mb-6">
          <h3 className="eyebrow mb-3 text-brass-500">Fellows present</h3>
          <AdminTable
            columns={cols}
            rows={partQ.data?.items ?? []}
            total={partQ.data?.total ?? 0}
            page={0}
            size={50}
            loading={partQ.isLoading}
            rowKey={(r) => r.userId}
            onRowClick={(r) => router.push(`/admin/users/${r.userId}`)}
            emptyTitle="No-one has joined yet"
          />
        </section>

        <section className="mt-8">
          {confirmDelete ? (
            <div className="flex flex-col gap-2">
              <p className="text-sm text-red-700">
                Adjourning permanently will cascade-delete every participation, affinity, and message.
              </p>
              <Button
                variant="danger"
                size="lg"
                loading={deleteEvent.isPending}
                onClick={() => deleteEvent.mutate()}
                fullWidth
              >
                Confirm adjournment
              </Button>
              <Button variant="ghost" onClick={() => setConfirmDelete(false)} fullWidth>
                Cancel
              </Button>
            </div>
          ) : (
            <Button variant="outline" onClick={() => setConfirmDelete(true)} fullWidth>
              <Trash2 className="mr-2 h-4 w-4" /> Adjourn permanently
            </Button>
          )}
        </section>
      </div>
    </AppShell>
  );
}
