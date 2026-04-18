"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";
import { adminApi, type AdminOutboxRow } from "@/lib/api/admin";
import { AppShell } from "@/components/layout/AppShell";
import { AdminSectionNav } from "@/components/admin/AdminSectionNav";
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
      key: "kind",
      header: "Kind",
      cell: (r) => (
        <div>
          <code className="text-xs">{r.kind}</code>
          <p className="mt-0.5 text-[10px] text-foreground/40">
            {new Date(r.createdAt).toLocaleString()}
          </p>
        </div>
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
        return (
          <div>
            <span className={`text-xs ${cls}`}>{r.status.toLowerCase()}</span>
            <p className="text-[10px] text-foreground/40">attempts {r.attempts}</p>
          </div>
        );
      }
    },
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
    <AppShell title="Apparatus" eyebrow="The Registry">
      <div className="flex-1 px-5 pt-6 pb-8">
        <AdminSectionNav />

        <header className="mb-5 flex items-end justify-between gap-3 hairline-b pb-5">
          <div>
            <p className="eyebrow text-brass-500">Mechanism &amp; pipework</p>
            <h2 className="mt-2 font-serif text-3xl leading-tight">
              <span className="italic">Apparatus</span>
            </h2>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              metrics.refetch();
              outbox.refetch();
            }}
          >
            <RefreshCw className="h-4 w-4" />
          </Button>
        </header>

        <section className="mb-6 grid grid-cols-2 gap-3">
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

        <div className="mb-4 flex flex-wrap gap-2">
          {(["", "PENDING", "FAILED", "DELIVERED"] as const).map((s) => (
            <button
              key={s || "all"}
              type="button"
              onClick={() => {
                setStatus(s);
                setPage(0);
              }}
              className={`eyebrow rounded-full border px-3 py-1 transition-colors ${
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
    </AppShell>
  );
}
