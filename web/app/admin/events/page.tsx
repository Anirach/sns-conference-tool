"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { adminApi, type AdminEventSummary } from "@/lib/api/admin";
import { AppShell } from "@/components/layout/AppShell";
import { AdminSectionNav } from "@/components/admin/AdminSectionNav";
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
    {
      key: "name",
      header: "Session",
      cell: (r) => (
        <div>
          <p className="font-serif text-base">{r.name}</p>
          <p className="mt-0.5 text-xs text-foreground/60">{r.venue}</p>
        </div>
      )
    },
    {
      key: "qr",
      header: "Cipher",
      cell: (r) => <code className="text-xs text-foreground/60">{r.qrCode}</code>
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
    <AppShell title="Sessions" eyebrow="The Registry">
      <div className="flex-1 px-5 pt-6 pb-8">
        <AdminSectionNav />

        <header className="mb-5 flex items-end justify-between gap-3 hairline-b pb-5">
          <div>
            <p className="eyebrow text-brass-500">In residence</p>
            <h2 className="mt-2 font-serif text-3xl leading-tight">
              <span className="italic">Sessions</span>
            </h2>
          </div>
          <Link href="/admin/events/new">
            <Button variant="primary" size="sm">
              <Plus className="mr-2 h-4 w-4" /> New
            </Button>
          </Link>
        </header>

        <div className="mb-4">
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
          emptyDescription="Create the first session to print ciphers for your event."
        />
      </div>
    </AppShell>
  );
}
