"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { AppShell } from "@/components/layout/AppShell";
import { useToast } from "@/components/ui/Toast";
import { authApi } from "@/lib/api/auth";
import { useAuthStore } from "@/lib/state/authStore";

const loginSchema = z.object({
  email: z.string().email("Enter a valid email address."),
  password: z.string().min(8, "At least 8 characters.")
});

const completeSchema = z.object({
  firstName: z.string().min(1, "Required"),
  lastName: z.string().min(1, "Required"),
  academicTitle: z.string().optional().default(""),
  institution: z.string().optional().default(""),
  password: z.string().min(8, "At least 8 characters.")
});

type LoginValues = z.infer<typeof loginSchema>;
type CompleteValues = z.infer<typeof completeSchema>;

function LoginInner() {
  const router = useRouter();
  const search = useSearchParams();
  const next = search.get("next");
  const verifiedEmail = search.get("email") ?? "";
  const { toast } = useToast();
  const setSession = useAuthStore((s) => s.setSession);
  const [mode] = useState<"login" | "complete">(next === "complete" ? "complete" : "login");

  const loginForm = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: verifiedEmail, password: "" }
  });
  const completeForm = useForm<CompleteValues>({
    resolver: zodResolver(completeSchema),
    defaultValues: { academicTitle: "", institution: "" }
  });

  async function onLogin(values: LoginValues) {
    try {
      const { data } = await authApi.login(values);
      await setSession(data);
      router.push("/events/join");
    } catch {
      toast({ title: "Login failed", variant: "error" });
    }
  }

  async function onComplete(values: CompleteValues) {
    try {
      const { data } = await authApi.complete(values);
      await setSession(data);
      toast({ title: "Account created", variant: "success" });
      router.push("/interests");
    } catch {
      toast({ title: "Could not create account", variant: "error" });
    }
  }

  if (mode === "complete") {
    return (
      <AppShell title="Complete profile" showBack hideTabs>
        <form className="flex flex-col gap-4" onSubmit={completeForm.handleSubmit(onComplete)}>
          <p className="text-sm text-gray-600">
            Fill in your details so others can find you. You can change these later.
          </p>
          <div className="grid grid-cols-2 gap-3">
            <Input label="First name" {...completeForm.register("firstName")} error={completeForm.formState.errors.firstName?.message} />
            <Input label="Last name" {...completeForm.register("lastName")} error={completeForm.formState.errors.lastName?.message} />
          </div>
          <Input label="Academic title" placeholder="Prof. / Dr. / PhD candidate" {...completeForm.register("academicTitle")} />
          <Input label="Institution" placeholder="MIT" {...completeForm.register("institution")} />
          <Input
            label="Password"
            type="password"
            autoComplete="new-password"
            {...completeForm.register("password")}
            error={completeForm.formState.errors.password?.message}
          />
          <Button type="submit" loading={completeForm.formState.isSubmitting}>
            Finish
          </Button>
        </form>
      </AppShell>
    );
  }

  return (
    <AppShell title="Log in" showBack hideTabs>
      <form className="flex flex-col gap-4" onSubmit={loginForm.handleSubmit(onLogin)}>
        <Input label="Email" type="email" autoComplete="email" {...loginForm.register("email")} error={loginForm.formState.errors.email?.message} />
        <Input
          label="Password"
          type="password"
          autoComplete="current-password"
          {...loginForm.register("password")}
          error={loginForm.formState.errors.password?.message}
        />
        <Button type="submit" loading={loginForm.formState.isSubmitting}>
          Log in
        </Button>
        <div className="flex justify-center gap-1 text-sm">
          <span className="text-gray-500">New here?</span>
          <Link href="/register" className="font-medium text-brand-600 hover:underline">
            Create an account
          </Link>
        </div>
      </form>
    </AppShell>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginInner />
    </Suspense>
  );
}
