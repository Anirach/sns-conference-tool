"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { adminApi, type AdminAuditEntry } from "@/lib/api/admin";
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
        <span className="text-xs tabular-nums">{new Date(r.createdAt).toLocaleString()}</span>
      )
    },
    { key: "action", header: "Action", cell: (r) => <code className="text-xs">{r.action}</code> },
    {
      key: "actor",
      header: "Actor",
      cell: (r) =>
        r.actorUserId ? (
          <code className="text-xs text-foreground/60">{r.actorUserId.substring(0, 8)}…</code>
        ) : (
          <span className="text-xs text-foreground/40">—</span>
        )
    },
    {
      key: "resource",
      header: "Resource",
      cell: (r) =>
        r.resourceType ? (
          <span className="text-xs text-foreground/80">
            {r.resourceType}/{r.resourceId?.substring(0, 8)}…
          </span>
        ) : (
          "—"
        )
    },
    {
      key: "payload",
      header: "Payload",
      cell: (r) =>
        r.payload ? (
          <code className="text-xs text-foreground/50">{r.payload.substring(0, 60)}</code>
        ) : (
          <span className="text-xs text-foreground/40">—</span>
        )
    }
  ];

  return (
    <div>
      <header className="mb-6">
        <p className="eyebrow text-brass-500">The immutable record</p>
        <h2 className="mt-2 font-serif text-3xl">Ledger</h2>
      </header>

      <div className="mb-4 grid gap-3 md:grid-cols-3">
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
      />
    </div>
  );
}
