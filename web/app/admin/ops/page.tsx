"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";
import { adminApi, type AdminOutboxRow } from "@/lib/api/admin";
import { AdminTable, type ColumnDef } from "@/components/admin/Table";
import { StatCard } from "@/components/admin/StatCard";
import { Button } from "@/components/ui/Button";

export default function AdminOpsPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<"" | "PENDING" | "DELIVERED" | "FAILED">("");

  const metrics = useQuery({
    queryKey: ["admin", "ops", "metrics"],
    queryFn: async () => (await adminApi.ops.metrics()).data,
    refetchInterval: 30_000
  });
  const outbox = useQuery({
    queryKey: ["admin", "ops", "outbox", page, status],
    queryFn: async () => (await adminApi.ops.outbox(page, 50, status || undefined)).data,
    refetchInterval: 30_000
  });

  const retry = useMutation({
    mutationFn: (outboxId: string) => adminApi.ops.retry(outboxId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "ops", "outbox"] })
  });

  const cols: ColumnDef<AdminOutboxRow>[] = [
    {
      key: "when",
      header: "Created",
      cell: (r) => (
        <span className="text-xs tabular-nums">{new Date(r.createdAt).toLocaleString()}</span>
      )
    },
    { key: "kind", header: "Kind", cell: (r) => <code className="text-xs">{r.kind}</code> },
    {
      key: "user",
      header: "User",
      cell: (r) => (
        <code className="text-xs text-foreground/60">{r.userId.substring(0, 8)}…</code>
      )
    },
    {
      key: "status",
      header: "Status",
      cell: (r) => {
        const cls =
          r.status === "DELIVERED"
            ? "text-brand-700"
            : r.status === "FAILED"
            ? "text-red-700"
            : "text-amber-700";
        return <span className={`text-xs ${cls}`}>{r.status.toLowerCase()}</span>;
      }
    },
    { key: "attempts", header: "Attempts", align: "right", cell: (r) => r.attempts },
    {
      key: "actions",
      header: "",
      align: "right",
      cell: (r) =>
        r.status === "FAILED" ? (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              retry.mutate(r.outboxId);
            }}
            className="inline-flex items-center gap-1 text-xs text-brand-700 hover:text-brand-500"
          >
            <RefreshCw className="h-3 w-3" /> Retry
          </button>
        ) : null
    }
  ];

  return (
    <div>
      <header className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="eyebrow text-brass-500">Mechanism &amp; pipework</p>
          <h2 className="mt-2 font-serif text-3xl">Apparatus</h2>
        </div>
        <Button
          variant="ghost"
          onClick={() => {
            metrics.refetch();
            outbox.refetch();
          }}
        >
          <RefreshCw className="mr-2 h-4 w-4" /> Refresh
        </Button>
      </header>

      <section className="mb-8 grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatCard
          label="Outbox pending"
          value={metrics.data?.outbox.pending ?? "…"}
          accent={metrics.data && metrics.data.outbox.pending > 50 ? "warn" : "default"}
        />
        <StatCard
          label="Outbox failed"
          value={metrics.data?.outbox.failed ?? "…"}
          accent={metrics.data && metrics.data.outbox.failed > 0 ? "danger" : "default"}
        />
        <StatCard label="Delivered (24h)" value={metrics.data?.outbox.delivered24h ?? "…"} />
        <StatCard label="Audit (24h)" value={metrics.data?.audit24h ?? "…"} />
      </section>

      <div className="mb-4 flex gap-2">
        {(["", "PENDING", "FAILED", "DELIVERED"] as const).map((s) => (
          <button
            key={s || "all"}
            type="button"
            onClick={() => {
              setStatus(s);
              setPage(0);
            }}
            className={`rounded-full border px-3 py-1 text-xs transition-colors ${
              status === s
                ? "border-brass-500 bg-brass-500/10 text-brass-700"
                : "border-border/60 text-foreground/60"
            }`}
          >
            {s ? s.toLowerCase() : "all"}
          </button>
        ))}
      </div>

      <AdminTable
        columns={cols}
        rows={outbox.data?.items ?? []}
        total={outbox.data?.total ?? 0}
        page={page}
        size={50}
        loading={outbox.isLoading}
        rowKey={(r) => r.outboxId}
        onPageChange={setPage}
        emptyTitle="Quiet on the line"
        emptyDescription="No push outbox rows match the selected status."
      />
    </div>
  );
}
