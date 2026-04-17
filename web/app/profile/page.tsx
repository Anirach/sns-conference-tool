"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect } from "react";
import { ChevronRight, Link2 } from "lucide-react";
import { UserAvatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { AppShell } from "@/components/layout/AppShell";
import { useToast } from "@/components/ui/Toast";
import { profileApi } from "@/lib/api/profile";
import type { User } from "@/lib/fixtures/types";

const schema = z.object({
  firstName: z.string().min(1, "Required"),
  lastName: z.string().min(1, "Required"),
  academicTitle: z.string().optional().default(""),
  institution: z.string().optional().default("")
});

type FormValues = z.infer<typeof schema>;

export default function ProfilePage() {
  const qc = useQueryClient();
  const { toast } = useToast();
  const { data, isLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: async () => (await profileApi.get()).data
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty, isSubmitting }
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (data) {
      reset({
        firstName: data.firstName,
        lastName: data.lastName,
        academicTitle: data.academicTitle ?? "",
        institution: data.institution ?? ""
      });
    }
  }, [data, reset]);

  const mutation = useMutation({
    mutationFn: (v: FormValues) => profileApi.update(v).then((r) => r.data),
    onSuccess: (u: User) => {
      qc.setQueryData(["profile"], u);
      toast({ title: "Particulars updated", variant: "success" });
      reset({
        firstName: u.firstName,
        lastName: u.lastName,
        academicTitle: u.academicTitle ?? "",
        institution: u.institution ?? ""
      });
    }
  });

  return (
    <AppShell title="Profile" eyebrow="The Registrar">
      <div className="flex-1 px-5 pt-5 pb-8">
        {isLoading || !data ? (
          <div className="py-20 text-center text-sm text-muted-foreground">Loading…</div>
        ) : (
          <>
            <div className="flex items-center gap-4 hairline-b pb-5">
              <UserAvatar
                firstName={data.firstName}
                lastName={data.lastName}
                src={data.profilePictureUrl}
                size={64}
              />
              <div className="min-w-0">
                <p className="eyebrow text-brass-500">{data.academicTitle || "Fellow"}</p>
                <h2 className="mt-1 truncate font-serif text-xl leading-tight text-foreground">
                  {data.firstName} {data.lastName}
                </h2>
                <p className="truncate font-serif text-xs italic text-muted-foreground">{data.email}</p>
              </div>
            </div>

            <form className="mt-6 flex flex-col gap-4" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
              <p className="eyebrow text-brass-500">Particulars</p>
              <div className="grid grid-cols-2 gap-3">
                <Input label="First name" {...register("firstName")} error={errors.firstName?.message} />
                <Input label="Last name" {...register("lastName")} error={errors.lastName?.message} />
              </div>
              <Input label="Academic title" placeholder="Prof. / Dr." {...register("academicTitle")} />
              <Input label="Institution" {...register("institution")} />
              <Button type="submit" disabled={!isDirty} loading={isSubmitting || mutation.isPending} size="lg">
                Save
              </Button>
            </form>

            <Link
              href="/profile/sns"
              className="mt-6 flex items-center justify-between gap-3 py-4 hairline-b hairline-t hover:bg-surface-muted"
            >
              <div className="flex items-center gap-3">
                <span className="grid h-9 w-9 place-items-center bg-brand-50 text-brand-500 hairline">
                  <Link2 className="h-4 w-4" strokeWidth={1.5} />
                </span>
                <div>
                  <p className="font-serif text-sm text-foreground">Linked Societies</p>
                  <p className="font-serif text-xs italic text-muted-foreground">Facebook · LinkedIn</p>
                </div>
              </div>
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            </Link>
          </>
        )}
      </div>
    </AppShell>
  );
}
