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
      // Admins land on the participant home like everyone else; a separate Registry tab
      // in BottomTabBar takes them to /admin when they want it. An explicit ?redirect=
      // still wins (the AdminLayout passes it when a non-authed visitor lands on /admin).
      router.push(search.get("redirect") ?? "/events/join");
    } catch {
      toast({ title: "Admission refused", variant: "error" });
    }
  }

  async function onComplete(values: CompleteValues) {
    try {
      const { data } = await authApi.complete(values);
      await setSession(data);
      toast({ title: "Enrolment complete", variant: "success" });
      router.push("/interests");
    } catch {
      toast({ title: "Enrolment refused", variant: "error" });
    }
  }

  if (mode === "complete") {
    return (
      <AppShell title="Your Particulars" eyebrow="Enrolment" showBack hideTabs>
        <div className="flex-1 px-5 pt-6 pb-8">
          <header className="mb-6 hairline-b pb-5">
            <p className="eyebrow text-brass-500">The Registrar</p>
            <h2 className="mt-2 font-serif text-2xl leading-tight text-foreground">
              A few <span className="italic">particulars</span>.
            </h2>
            <p className="mt-2 font-serif text-sm italic text-muted-foreground">
              Others will recognise you by these.
            </p>
          </header>

          <form className="flex flex-col gap-4" onSubmit={completeForm.handleSubmit(onComplete)}>
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="First name"
                {...completeForm.register("firstName")}
                error={completeForm.formState.errors.firstName?.message}
              />
              <Input
                label="Last name"
                {...completeForm.register("lastName")}
                error={completeForm.formState.errors.lastName?.message}
              />
            </div>
            <Input
              label="Academic title"
              placeholder="Prof. / Dr. / PhD candidate"
              {...completeForm.register("academicTitle")}
            />
            <Input label="Institution" placeholder="MIT" {...completeForm.register("institution")} />
            <Input
              label="Password"
              type="password"
              autoComplete="new-password"
              {...completeForm.register("password")}
              error={completeForm.formState.errors.password?.message}
            />
            <Button type="submit" loading={completeForm.formState.isSubmitting} size="lg" fullWidth>
              Confirm Enrolment
            </Button>
          </form>
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="A Returning Fellow" eyebrow="Admission" showBack hideTabs>
      <div className="flex-1 px-5 pt-6 pb-8">
        <header className="mb-6 hairline-b pb-5">
          <p className="eyebrow text-brass-500">Welcome</p>
          <h2 className="mt-2 font-serif text-2xl leading-tight text-foreground">
            Good to see you <span className="italic">again</span>.
          </h2>
        </header>

        <form className="flex flex-col gap-4" onSubmit={loginForm.handleSubmit(onLogin)}>
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            {...loginForm.register("email")}
            error={loginForm.formState.errors.email?.message}
          />
          <Input
            label="Password"
            type="password"
            autoComplete="current-password"
            {...loginForm.register("password")}
            error={loginForm.formState.errors.password?.message}
          />
          <Button type="submit" loading={loginForm.formState.isSubmitting} size="lg" fullWidth>
            Grant Admission
          </Button>
          <div className="flex justify-center gap-1 text-sm">
            <span className="font-serif italic text-muted-foreground">New here?</span>
            <Link href="/register" className="font-serif italic text-brass-600 hover:text-brass-700">
              Request admission
            </Link>
          </div>
        </form>
      </div>
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
