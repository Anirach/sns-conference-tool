"use client";

import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Trash2 } from "lucide-react";
import { adminApi } from "@/lib/api/admin";
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
        <span>
          {r.firstName ?? "—"} {r.lastName ?? ""}
        </span>
      )
    },
    { key: "inst", header: "Institution", cell: (r) => r.institution ?? "—" },
    {
      key: "pos",
      header: "Position",
      cell: (r) =>
        r.lastLat && r.lastLon ? (
          <code className="text-xs text-foreground/60">
            {r.lastLat.toFixed(4)}, {r.lastLon.toFixed(4)}
          </code>
        ) : (
          <span className="text-xs text-foreground/40">no fix</span>
        )
    },
    {
      key: "update",
      header: "Last update",
      cell: (r) =>
        r.lastUpdate ? (
          <span className="text-xs tabular-nums">{new Date(r.lastUpdate).toLocaleTimeString()}</span>
        ) : (
          "—"
        )
    },
    { key: "radius", header: "Radius", align: "right", cell: (r) => `${r.selectedRadius} m` }
  ];

  const detail = detailQ.data;

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="eyebrow text-brass-500">Session</p>
          <h2 className="mt-2 font-serif text-3xl">
            {detail ? detail.name : <span className="text-foreground/30">…</span>}
          </h2>
          <p className="mt-1 font-serif text-sm italic text-muted-foreground">
            {detail?.venue} · cipher <code className="not-italic">{detail?.qrCode}</code>
          </p>
        </div>
        <div className="flex gap-2">
          {confirmDelete ? (
            <>
              <Button variant="danger" loading={deleteEvent.isPending} onClick={() => deleteEvent.mutate()}>
                Confirm adjournment
              </Button>
              <Button variant="ghost" onClick={() => setConfirmDelete(false)}>
                Cancel
              </Button>
            </>
          ) : (
            <Button variant="outline" onClick={() => setConfirmDelete(true)}>
              <Trash2 className="mr-2 h-4 w-4" /> Adjourn permanently
            </Button>
          )}
        </div>
      </header>

      <section className="mb-6 grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatCard label="Fellows in residence" value={detail?.participantCount ?? 0} />
        <StatCard label="Affinities" value={detail?.matchCount ?? 0} />
        <StatCard label="Correspondence" value={detail?.messageCount ?? 0} />
        <StatCard
          label="Status"
          value={detail?.expired ? "Adjourned" : "In residence"}
          accent={detail?.expired ? "warn" : "default"}
        />
      </section>

      <section className="mb-6 grid gap-6 md:grid-cols-[320px_1fr]">
        <div>
          <h3 className="eyebrow mb-2 text-brass-500">Venue heatmap</h3>
          <VenueHeatmap
            points={heatQ.data ?? []}
            centroidLat={detail?.centroidLat}
            centroidLon={detail?.centroidLon}
          />
        </div>
        <div>
          <h3 className="eyebrow mb-2 text-brass-500">Fellows present</h3>
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
        </div>
      </section>
    </div>
  );
}
