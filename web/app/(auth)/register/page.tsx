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
      toast({ title: "A cipher is in transit", description: "Check your inbox for the six-digit code.", variant: "success" });
      router.push(`/verify?email=${encodeURIComponent(values.email)}`);
    } catch {
      toast({ title: "Request refused", description: "Please try again shortly.", variant: "error" });
    }
  }

  return (
    <AppShell title="Request Admission" eyebrow="Enrolment" showBack hideTabs>
      <div className="flex-1 px-5 pt-6 pb-8">
        <header className="mb-6 hairline-b pb-5">
          <p className="eyebrow text-brass-500">The Registrar</p>
          <h2 className="mt-2 font-serif text-2xl leading-tight text-foreground">
            Let us begin your <span className="italic">enrolment</span>.
          </h2>
          <p className="mt-2 font-serif text-sm italic text-muted-foreground">
            We shall dispatch a cipher to verify your address.
          </p>
        </header>

        <form className="flex flex-col gap-5" onSubmit={handleSubmit(onSubmit)}>
          <Input
            label="Email"
            type="email"
            autoComplete="email"
            inputMode="email"
            placeholder="you@university.edu"
            error={errors.email?.message}
            {...register("email")}
          />
          <Button type="submit" loading={isSubmitting} size="lg" fullWidth>
            Dispatch Cipher
          </Button>
        </form>
      </div>
    </AppShell>
  );
}
