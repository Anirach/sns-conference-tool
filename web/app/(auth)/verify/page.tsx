"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { Suspense } from "react";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { AppShell } from "@/components/layout/AppShell";
import { useToast } from "@/components/ui/Toast";
import { authApi } from "@/lib/api/auth";

const schema = z.object({
  tan: z.string().regex(/^\d{6}$/u, "Enter the 6-digit code we emailed you.")
});

type FormValues = z.infer<typeof schema>;

function VerifyInner() {
  const router = useRouter();
  const search = useSearchParams();
  const email = search.get("email") ?? "";
  const { toast } = useToast();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    try {
      await authApi.verify({ email, tan: values.tan });
      toast({ title: "Email verified", variant: "success" });
      router.push(`/login?verified=1&email=${encodeURIComponent(email)}&next=complete`);
    } catch {
      toast({ title: "Invalid code", variant: "error" });
    }
  }

  return (
    <AppShell title="Verify email" subtitle={email || undefined} showBack hideTabs>
      <form className="flex flex-col gap-5" onSubmit={handleSubmit(onSubmit)}>
        <p className="text-sm text-gray-600">Paste or type the 6-digit code from your inbox.</p>
        <Input
          label="Verification code"
          inputMode="numeric"
          autoComplete="one-time-code"
          placeholder="123456"
          maxLength={6}
          error={errors.tan?.message}
          {...register("tan")}
        />
        <Button type="submit" loading={isSubmitting}>
          Verify
        </Button>
      </form>
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
