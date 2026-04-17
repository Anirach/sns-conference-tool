"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Link as LinkIcon, FileText, Type, Trash2, Sparkles } from "lucide-react";
import { useState } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/Card";
import { Dialog } from "@/components/ui/Dialog";
import { Input, Textarea } from "@/components/ui/Input";
import { EmptyState } from "@/components/ui/EmptyState";
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
  const [activeTab, setActiveTab] = useState<InterestType>("TEXT");
  const [textValue, setTextValue] = useState("");
  const [linkValue, setLinkValue] = useState("");
  const [pickedFile, setPickedFile] = useState<FilePickResult | null>(null);

  const createMut = useMutation({
    mutationFn: (body: { type: InterestType; content: string }) =>
      interestsApi.create(body).then((r) => r.data),
    onSuccess: (created) => {
      qc.setQueryData<Interest[]>(["interests"], (prev = []) => [created, ...prev]);
      toast({ title: "Interest added", description: `Extracted ${created.extractedKeywords.length} keywords.`, variant: "success" });
      reset();
    },
    onError: () => toast({ title: "Could not add interest", variant: "error" })
  });

  const deleteMut = useMutation({
    mutationFn: (id: string) => interestsApi.delete(id),
    onSuccess: (_res, id) => {
      qc.setQueryData<Interest[]>(["interests"], (prev = []) => prev.filter((i) => i.interestId !== id));
    }
  });

  function reset() {
    setOpen(false);
    setTextValue("");
    setLinkValue("");
    setPickedFile(null);
    setActiveTab("TEXT");
  }

  async function onPickFile() {
    try {
      const res = await bridge.call<FilePickResult>("file.pickArticle", { allowedExt: ["pdf", "txt"] });
      setPickedFile(res);
    } catch {
      toast({ title: "Could not open file picker", variant: "error" });
    }
  }

  function submit() {
    if (activeTab === "TEXT") {
      if (!textValue.trim()) return;
      createMut.mutate({ type: "TEXT", content: textValue });
    } else if (activeTab === "ARTICLE_LINK") {
      if (!linkValue.trim()) return;
      createMut.mutate({ type: "ARTICLE_LINK", content: linkValue });
    } else {
      if (!pickedFile) return;
      createMut.mutate({ type: "ARTICLE_LOCAL", content: pickedFile.path });
    }
  }

  return (
    <AppShell title="Interests" right={<AddButton onClick={() => setOpen(true)} />}>
      {isLoading ? (
        <div className="py-12 text-center text-sm text-gray-500">Loading…</div>
      ) : interests.length === 0 ? (
        <EmptyState
          icon={Sparkles}
          title="Add your first interest"
          description="Paste a paragraph, drop a PDF, or share a link. We'll extract keywords to match you with others."
          action={<Button onClick={() => setOpen(true)}>Add interest</Button>}
        />
      ) : (
        <div className="flex flex-col gap-3">
          {interests.map((i) => (
            <Card key={i.interestId}>
              <CardHeader>
                <div className="flex items-start justify-between gap-2">
                  <CardTitle className="flex items-center gap-2 text-base">
                    <TypeBadge type={i.type} />
                  </CardTitle>
                  <button
                    type="button"
                    aria-label="Delete"
                    onClick={() => deleteMut.mutate(i.interestId)}
                    className="rounded-full p-1 text-gray-400 hover:bg-gray-100 hover:text-red-600"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
                <CardDescription className="break-words">{contentPreview(i)}</CardDescription>
              </CardHeader>
              <CardFooter className="flex-wrap gap-1.5">
                {i.extractedKeywords.map((k) => (
                  <Chip key={k} variant="brand">
                    {k}
                  </Chip>
                ))}
              </CardFooter>
            </Card>
          ))}
        </div>
      )}

      <Dialog
        open={open}
        onOpenChange={(o) => (o ? setOpen(true) : reset())}
        title="Add interest"
        description="Choose how you want to describe a research topic."
        footer={
          <>
            <Button variant="secondary" onClick={reset}>
              Cancel
            </Button>
            <Button onClick={submit} loading={createMut.isPending}>
              Extract &amp; save
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-3 gap-2">
            <TabButton active={activeTab === "TEXT"} onClick={() => setActiveTab("TEXT")} icon={<Type className="h-4 w-4" />} label="Text" />
            <TabButton active={activeTab === "ARTICLE_LINK"} onClick={() => setActiveTab("ARTICLE_LINK")} icon={<LinkIcon className="h-4 w-4" />} label="Link" />
            <TabButton active={activeTab === "ARTICLE_LOCAL"} onClick={() => setActiveTab("ARTICLE_LOCAL")} icon={<FileText className="h-4 w-4" />} label="File" />
          </div>

          {activeTab === "TEXT" && (
            <Textarea
              label="Describe your research interest"
              placeholder="e.g. I work on graph neural networks for drug–target interaction prediction…"
              value={textValue}
              onChange={(e) => setTextValue(e.target.value)}
            />
          )}

          {activeTab === "ARTICLE_LINK" && (
            <Input
              label="Link (arXiv, DOI, or any URL)"
              type="url"
              placeholder="https://arxiv.org/abs/2305.12345"
              value={linkValue}
              onChange={(e) => setLinkValue(e.target.value)}
              hint="We'll fetch the abstract to extract keywords."
            />
          )}

          {activeTab === "ARTICLE_LOCAL" && (
            <div className="flex flex-col gap-3">
              <Button variant="secondary" onClick={onPickFile}>
                {pickedFile ? "Choose a different file" : "Choose PDF or TXT"}
              </Button>
              {pickedFile ? (
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-3 text-xs text-gray-600">
                  <div className="font-medium text-gray-800">{pickedFile.name}</div>
                  <div>{(pickedFile.sizeBytes / 1024).toFixed(1)} KB</div>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </Dialog>
    </AppShell>
  );
}

function AddButton({ onClick }: { onClick: () => void }) {
  return (
    <Button size="sm" onClick={onClick} className="gap-1.5">
      <Plus className="h-4 w-4" />
      Add
    </Button>
  );
}

function TabButton({
  active,
  onClick,
  icon,
  label
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={
        "flex flex-col items-center gap-1 rounded-xl border px-3 py-2 text-xs transition-colors " +
        (active ? "border-brand-500 bg-brand-50 text-brand-700" : "border-gray-200 text-gray-600 hover:border-gray-300")
      }
    >
      {icon}
      {label}
    </button>
  );
}

function TypeBadge({ type }: { type: InterestType }) {
  const label = type === "TEXT" ? "Text" : type === "ARTICLE_LINK" ? "Link" : "File";
  const icon = type === "TEXT" ? <Type className="h-3.5 w-3.5" /> : type === "ARTICLE_LINK" ? <LinkIcon className="h-3.5 w-3.5" /> : <FileText className="h-3.5 w-3.5" />;
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-medium text-gray-600">
      {icon}
      {label}
    </span>
  );
}

function contentPreview(i: Interest): string {
  if (i.type === "TEXT") return i.content;
  if (i.type === "ARTICLE_LINK") return i.content;
  return i.content.split("/").pop() || i.content;
}
