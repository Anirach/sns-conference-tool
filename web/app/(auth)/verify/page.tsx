"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { Button } from "@/components/ui/Button";
import { AppShell } from "@/components/layout/AppShell";
import { TanInput } from "@/components/onboarding/TanInput";
import { useToast } from "@/components/ui/Toast";
import { authApi } from "@/lib/api/auth";

function VerifyInner() {
  const router = useRouter();
  const search = useSearchParams();
  const email = search.get("email") ?? "";
  const { toast } = useToast();
  const [code, setCode] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onComplete(value: string) {
    setCode(value);
    setBusy(true);
    try {
      await authApi.verify({ email, tan: value });
      toast({ title: "Cipher accepted", variant: "success" });
      router.push(`/login?verified=1&email=${encodeURIComponent(email)}&next=complete`);
    } catch {
      toast({ title: "Cipher refused", variant: "error" });
      setBusy(false);
    }
  }

  return (
    <AppShell title="Verify Cipher" eyebrow={email || "Enrolment"} showBack hideTabs>
      <div className="flex-1 px-5 pt-8 pb-8">
        <header className="mb-8 text-center">
          <p className="eyebrow text-brass-500">Correspondence Awaits</p>
          <h2 className="mt-2 font-serif text-2xl leading-tight text-foreground">
            Transcribe the <span className="italic">six-digit</span> cipher.
          </h2>
          <p className="mt-2 font-serif text-sm italic text-muted-foreground">
            We dispatched it to <span className="not-italic">{email || "your address"}</span>.
          </p>
        </header>

        <TanInput onComplete={onComplete} disabled={busy} />

        <p className="mt-8 text-center text-xs text-muted-foreground">
          Didn&apos;t receive it?{" "}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => toast({ title: "Cipher re-issued (mock)" })}
            className="inline-flex h-auto px-1 py-0 font-serif text-xs italic"
          >
            Request another
          </Button>
        </p>

        {code ? <div className="mt-4 text-center text-[10px] text-muted-foreground tabular-nums">{code}</div> : null}
      </div>
    </AppShell>
  );
}

export default function VerifyPage() {
  return (
    <Suspense fallback={null}>
      <VerifyInner />
    </Suspense>
  );
}
