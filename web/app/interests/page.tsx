"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BookOpen, FileText, Link as LinkIcon, Plus, Trash2, Type, Upload } from "lucide-react";
import { useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { Input, Textarea } from "@/components/ui/Input";
import { KeywordChip } from "@/components/ui/Chip";
import { EmptyState } from "@/components/ui/EmptyState";
import { Dialog } from "@/components/ui/Dialog";
import { useToast } from "@/components/ui/Toast";
import { interestsApi } from "@/lib/api/interests";
import { bridge } from "@/lib/bridge/client";
import type { Interest, InterestType } from "@/lib/fixtures/types";
import type { FilePickResult } from "@/lib/bridge/types";

export default function InterestsPage() {
  const qc = useQueryClient();
  const { toast } = useToast();
  const { data: interests = [], isLoading } = useQuery<Interest[]>({
    queryKey: ["interests"],
    queryFn: async () => (await interestsApi.list()).data
  });

  const [open, setOpen] = useState(false);
  const [tab, setTab] = useState<InterestType>("TEXT");
  const [text, setText] = useState("");
  const [link, setLink] = useState("");
  const [picked, setPicked] = useState<FilePickResult | null>(null);

  const createMut = useMutation({
    mutationFn: (body: { type: InterestType; content: string }) =>
      interestsApi.create(body).then((r) => r.data),
    onSuccess: (created) => {
      qc.setQueryData<Interest[]>(["interests"], (prev = []) => [created, ...prev]);
      toast({
        title: "Inquiry entered",
        description: `Extracted ${created.extractedKeywords.length} key terms.`,
        variant: "success"
      });
      reset();
    },
    onError: () => toast({ title: "Could not enter inquiry", variant: "error" })
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => interestsApi.delete(id),
    onSuccess: (_r, id) => {
      qc.setQueryData<Interest[]>(["interests"], (prev = []) => prev.filter((i) => i.interestId !== id));
    }
  });

  function reset() {
    setOpen(false);
    setText("");
    setLink("");
    setPicked(null);
    setTab("TEXT");
  }

  async function onPick() {
    try {
      const res = await bridge.call<FilePickResult>("file.pickArticle", { allowedExt: ["pdf", "txt"] });
      setPicked(res);
    } catch {
      toast({ title: "File picker unavailable", variant: "error" });
    }
  }

  function submit() {
    if (tab === "TEXT" && text.trim()) {
      createMut.mutate({ type: "TEXT", content: text });
    } else if (tab === "ARTICLE_LINK" && link.trim()) {
      createMut.mutate({ type: "ARTICLE_LINK", content: link });
    } else if (tab === "ARTICLE_LOCAL" && picked) {
      createMut.mutate({ type: "ARTICLE_LOCAL", content: picked.path });
    }
  }

  return (
    <AppShell
      title="Inquiries"
      eyebrow="The Register"
      right={
        <Button size="sm" onClick={() => setOpen(true)} className="gap-1.5">
          <Plus className="h-3.5 w-3.5" strokeWidth={1.5} />
          Add
        </Button>
      }
    >
      <div className="flex-1 px-5 pt-5 pb-8">
        <header className="mb-5 hairline-b pb-5">
          <p className="eyebrow text-brass-500">Curated</p>
          <h2 className="mt-2 font-serif text-2xl leading-tight text-foreground">
            Topics of <span className="italic">Inquiry</span>
          </h2>
          <p className="mt-1 font-serif text-xs italic text-muted-foreground">
            These shape the affinities we surface.
          </p>
        </header>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">Loading…</div>
        ) : interests.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            title="No inquiries yet"
            description="Paste a paragraph, deposit a PDF, or link to an arXiv abstract."
            ctaLabel="Add inquiry"
            onCta={() => setOpen(true)}
          />
        ) : (
          <div>
            {interests.map((i) => (
              <article key={i.interestId} className="py-4 hairline-b">
                <div className="mb-2 flex items-start justify-between gap-2">
                  <TypeBadge type={i.type} />
                  <button
                    type="button"
                    onClick={() => deleteMut.mutate(i.interestId)}
                    aria-label="Remove inquiry"
                    className="p-1 text-foreground/40 hover:text-danger"
                  >
                    <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
                  </button>
                </div>
                <p className="font-serif text-sm leading-relaxed text-foreground/80 break-words">
                  {preview(i)}
                </p>
                <div className="mt-3 flex flex-wrap gap-x-2 gap-y-1">
                  {i.extractedKeywords.map((k, idx) => (
                    <span key={k} className="inline-flex items-center gap-2">
                      {idx > 0 ? <span className="text-foreground/20">/</span> : null}
                      <KeywordChip label={k} />
                    </span>
                  ))}
                </div>
              </article>
            ))}
          </div>
        )}
      </div>

      <Dialog
        open={open}
        onOpenChange={(o) => (o ? setOpen(true) : reset())}
        eyebrow="New Inquiry"
        title="Enter a topic"
        description="Choose how to describe it. We'll extract the key terms."
        footer={
          <>
            <Button variant="outline" onClick={reset}>
              Cancel
            </Button>
            <Button onClick={submit} loading={createMut.isPending}>
              Enter
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          <div className="flex gap-2 hairline-b pb-2">
            {(
              [
                ["TEXT", "Text", Type],
                ["ARTICLE_LINK", "Link", LinkIcon],
                ["ARTICLE_LOCAL", "File", Upload]
              ] as const
            ).map(([key, label, Icon]) => {
              const active = tab === key;
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => setTab(key)}
                  className={`eyebrow inline-flex items-center gap-1.5 border-b pb-1.5 transition-colors ${
                    active
                      ? "border-brass-500 text-brass-500"
                      : "border-transparent text-foreground/40 hover:text-foreground"
                  }`}
                >
                  <Icon className="h-3 w-3" strokeWidth={1.5} />
                  {label}
                </button>
              );
            })}
          </div>

          {tab === "TEXT" ? (
            <Textarea
              label="Describe your research"
              placeholder="e.g. I work on graph neural networks for drug–target interaction prediction…"
              value={text}
              onChange={(e) => setText(e.target.value)}
              hint={`${text.length}/500`}
            />
          ) : null}

          {tab === "ARTICLE_LINK" ? (
            <Input
              label="arXiv or Google Scholar URL"
              type="url"
              placeholder="https://arxiv.org/abs/2305.12345"
              value={link}
              onChange={(e) => setLink(e.target.value)}
              hint="We&apos;ll fetch the abstract to extract keywords."
            />
          ) : null}

          {tab === "ARTICLE_LOCAL" ? (
            <div className="flex flex-col gap-3">
              <button
                type="button"
                onClick={onPick}
                className="flex cursor-pointer flex-col items-center gap-2 bg-card p-8 text-center hairline hover:bg-surface-muted"
              >
                <BookOpen className="h-8 w-8 text-brand-500" strokeWidth={1.4} />
                <p className="font-serif text-sm text-foreground">
                  {picked ? picked.name : "Deposit a PDF / TXT"}
                </p>
                <p className="text-[10px] uppercase tracking-[0.14em] text-muted-foreground">Max 10 MB</p>
              </button>
              {picked ? (
                <p className="text-[10px] uppercase tracking-[0.14em] text-muted-foreground">
                  {(picked.sizeBytes / 1024).toFixed(1)} KB
                </p>
              ) : null}
            </div>
          ) : null}
        </div>
      </Dialog>
    </AppShell>
  );
}

function TypeBadge({ type }: { type: InterestType }) {
  const label = type === "TEXT" ? "Text" : type === "ARTICLE_LINK" ? "Link" : "File";
  const Icon = type === "TEXT" ? Type : type === "ARTICLE_LINK" ? LinkIcon : FileText;
  return (
    <span className="eyebrow inline-flex items-center gap-1 text-brass-500">
      <Icon className="h-3 w-3" strokeWidth={1.5} />
      {label}
    </span>
  );
}

function preview(i: Interest): string {
  if (i.type === "TEXT") return i.content;
  if (i.type === "ARTICLE_LINK") return i.content;
  return i.content.split("/").pop() || i.content;
}
