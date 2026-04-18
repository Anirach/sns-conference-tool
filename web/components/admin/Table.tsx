"use client";

import { ReactNode } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

export interface ColumnDef<T> {
  key: string;
  header: ReactNode;
  cell: (row: T) => ReactNode;
  align?: "left" | "right";
  width?: string;
}

interface AdminTableProps<T> {
  columns: ColumnDef<T>[];
  rows: T[];
  total: number;
  page: number;
  size: number;
  onPageChange?: (page: number) => void;
  loading?: boolean;
  emptyTitle?: string;
  emptyDescription?: string;
  rowKey: (row: T) => string;
  onRowClick?: (row: T) => void;
}

export function AdminTable<T>({
  columns,
  rows,
  total,
  page,
  size,
  onPageChange,
  loading,
  emptyTitle = "Nothing to show",
  emptyDescription,
  rowKey,
  onRowClick
}: AdminTableProps<T>) {
  const lastPage = Math.max(0, Math.ceil(total / size) - 1);
  const start = total === 0 ? 0 : page * size + 1;
  const end = Math.min((page + 1) * size, total);

  return (
    <div className="hairline rounded-md bg-surface">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="hairline-b">
              {columns.map((c) => (
                <th
                  key={c.key}
                  scope="col"
                  className={`eyebrow px-4 py-3 text-foreground/60 ${
                    c.align === "right" ? "text-right" : "text-left"
                  }`}
                  style={c.width ? { width: c.width } : undefined}
                >
                  {c.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading
              ? Array.from({ length: 5 }).map((_, i) => (
                  <tr key={`skel-${i}`} className="hairline-b">
                    {columns.map((c) => (
                      <td key={c.key} className="px-4 py-4">
                        <div className="h-3 w-3/4 rounded bg-surface-muted" />
                      </td>
                    ))}
                  </tr>
                ))
              : rows.length === 0
              ? null
              : rows.map((row) => (
                  <tr
                    key={rowKey(row)}
                    onClick={onRowClick ? () => onRowClick(row) : undefined}
                    className={`hairline-b transition-colors ${
                      onRowClick ? "cursor-pointer hover:bg-surface-muted" : ""
                    }`}
                  >
                    {columns.map((c) => (
                      <td
                        key={c.key}
                        className={`px-4 py-3 ${c.align === "right" ? "text-right" : ""}`}
                      >
                        {c.cell(row)}
                      </td>
                    ))}
                  </tr>
                ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <p className="eyebrow text-brass-500">Nota Bene</p>
            <h3 className="mt-2 font-serif text-xl">{emptyTitle}</h3>
            {emptyDescription ? (
              <p className="mt-2 text-sm text-muted-foreground">{emptyDescription}</p>
            ) : null}
          </div>
        ) : null}
      </div>
      {total > 0 ? (
        <div className="flex items-center justify-between px-4 py-3 hairline-t text-xs">
          <span className="text-foreground/60 tabular-nums">
            {start}–{end} of {total}
          </span>
          {onPageChange ? (
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={page === 0}
                onClick={() => onPageChange(page - 1)}
                className="grid h-7 w-7 place-items-center rounded border border-border/60 disabled:opacity-30"
                aria-label="Previous page"
              >
                <ChevronLeft className="h-3.5 w-3.5" />
              </button>
              <span className="text-foreground/60 tabular-nums">
                {page + 1} / {lastPage + 1}
              </span>
              <button
                type="button"
                disabled={page >= lastPage}
                onClick={() => onPageChange(page + 1)}
                className="grid h-7 w-7 place-items-center rounded border border-border/60 disabled:opacity-30"
                aria-label="Next page"
              >
                <ChevronRight className="h-3.5 w-3.5" />
              </button>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
