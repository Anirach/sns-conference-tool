"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { adminApi, type AdminEventSummary } from "@/lib/api/admin";
import { AdminTable, type ColumnDef } from "@/components/admin/Table";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";

export default function AdminEventsListPage() {
  const router = useRouter();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "events", page, q],
    queryFn: async () => (await adminApi.events.list(page, 20, q || undefined)).data
  });

  const columns: ColumnDef<AdminEventSummary>[] = [
    { key: "name", header: "Session", cell: (r) => <span className="font-serif text-base">{r.name}</span> },
    { key: "venue", header: "Venue", cell: (r) => <span className="text-foreground/80">{r.venue}</span> },
    {
      key: "qr",
      header: "Cipher",
      cell: (r) => <code className="text-xs text-foreground/60">{r.qrCode}</code>
    },
    {
      key: "expires",
      header: "Adjourns",
      cell: (r) => (
        <span className="text-xs tabular-nums text-foreground/60">
          {new Date(r.expirationCode).toLocaleString()}
        </span>
      )
    },
    {
      key: "status",
      header: "Status",
      cell: (r) =>
        r.expired ? (
          <span className="text-xs text-red-700">Adjourned</span>
        ) : (
          <span className="text-xs text-brand-700">In residence</span>
        )
    },
    {
      key: "participants",
      header: "Fellows",
      align: "right",
      cell: (r) => <span className="tabular-nums">{r.participantCount}</span>
    }
  ];

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="eyebrow text-brass-500">In residence</p>
          <h2 className="mt-2 font-serif text-3xl">Sessions</h2>
        </div>
        <Link href="/admin/events/new">
          <Button variant="primary">
            <Plus className="mr-2 h-4 w-4" /> New session
          </Button>
        </Link>
      </header>

      <div className="mb-4 max-w-xs">
        <Input
          placeholder="Search by name, venue, cipher…"
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setPage(0);
          }}
        />
      </div>

      <AdminTable
        columns={columns}
        rows={data?.items ?? []}
        total={data?.total ?? 0}
        page={page}
        size={20}
        loading={isLoading}
        rowKey={(r) => r.eventId}
        onPageChange={setPage}
        onRowClick={(r) => router.push(`/admin/events/${r.eventId}`)}
        emptyTitle="No sessions yet"
        emptyDescription="Create the first session to print QR codes for your event."
      />
    </div>
  );
}
