"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { adminApi, type AdminAuditEntry } from "@/lib/api/admin";
import { AppShell } from "@/components/layout/AppShell";
import { AdminSectionNav } from "@/components/admin/AdminSectionNav";
import { AdminTable, type ColumnDef } from "@/components/admin/Table";
import { Input } from "@/components/ui/Input";

export default function AdminAuditPage() {
  const [page, setPage] = useState(0);
  const [actor, setActor] = useState("");
  const [action, setAction] = useState("");
  const [since, setSince] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["admin", "audit", page, actor, action, since],
    queryFn: async () =>
      (
        await adminApi.audit.search(
          page,
          50,
          actor || undefined,
          action || undefined,
          since ? new Date(since).toISOString() : undefined
        )
      ).data
  });

  const cols: ColumnDef<AdminAuditEntry>[] = [
    {
      key: "when",
      header: "When",
      cell: (r) => (
        <span className="text-xs tabular-nums text-foreground/60">
          {new Date(r.createdAt).toLocaleString()}
        </span>
      )
    },
    {
      key: "action",
      header: "Action",
      cell: (r) => (
        <div>
          <code className="text-xs">{r.action}</code>
          {r.actorUserId ? (
            <p className="mt-0.5 text-[10px] text-foreground/40">
              actor {r.actorUserId.substring(0, 8)}…
            </p>
          ) : null}
        </div>
      )
    }
  ];

  return (
    <AppShell title="Ledger" eyebrow="The Registry">
      <div className="flex-1 px-5 pt-6 pb-8">
        <AdminSectionNav />

        <header className="mb-5 hairline-b pb-5">
          <p className="eyebrow text-brass-500">The immutable record</p>
          <h2 className="mt-2 font-serif text-3xl leading-tight">
            <span className="italic">Ledger</span>
          </h2>
        </header>

        <div className="mb-4 flex flex-col gap-3">
          <Input
            placeholder="Actor user id (UUID)…"
            value={actor}
            onChange={(e) => {
              setActor(e.target.value);
              setPage(0);
            }}
          />
          <Input
            placeholder="Action (e.g. auth.login.failure)…"
            value={action}
            onChange={(e) => {
              setAction(e.target.value);
              setPage(0);
            }}
          />
          <Input
            type="datetime-local"
            value={since}
            onChange={(e) => {
              setSince(e.target.value);
              setPage(0);
            }}
          />
        </div>

        <AdminTable
          columns={cols}
          rows={data?.items ?? []}
          total={data?.total ?? 0}
          page={page}
          size={50}
          loading={isLoading}
          rowKey={(r) => r.id}
          onPageChange={setPage}
          emptyTitle="No entries"
          emptyDescription="Loosen the filters to see more of the ledger."
        />
      </div>
    </AppShell>
  );
}
