"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { AppShell } from "@/components/layout/AppShell";
import { useToast } from "@/components/ui/Toast";
import { authApi } from "@/lib/api/auth";

const schema = z.object({
  email: z.string().email("Enter a valid email address.")
});

type FormValues = z.infer<typeof schema>;

export default function RegisterPage() {
  const router = useRouter();
  const { toast } = useToast();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    try {
      await authApi.register({ email: values.email });
      toast({ title: "Check your inbox", description: "We sent you a verification code.", variant: "success" });
      router.push(`/verify?email=${encodeURIComponent(values.email)}`);
    } catch {
      toast({ title: "Could not register", description: "Please try again shortly.", variant: "error" });
    }
  }

  return (
    <AppShell title="Create account" showBack hideTabs>
      <form className="flex flex-col gap-5" onSubmit={handleSubmit(onSubmit)}>
        <p className="text-sm text-gray-600">
          Enter your academic or work email. We&apos;ll send you a 6-digit code to verify it.
        </p>
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          inputMode="email"
          placeholder="you@university.edu"
          error={errors.email?.message}
          {...register("email")}
        />
        <Button type="submit" loading={isSubmitting}>
          Send verification code
        </Button>
      </form>
    </AppShell>
  );
}
