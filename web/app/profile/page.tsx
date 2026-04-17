"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useEffect } from "react";
import { Link2 } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
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
      toast({ title: "Profile saved", variant: "success" });
      reset({
        firstName: u.firstName,
        lastName: u.lastName,
        academicTitle: u.academicTitle ?? "",
        institution: u.institution ?? ""
      });
    }
  });

  return (
    <AppShell title="Profile">
      {isLoading || !data ? (
        <div className="flex items-center justify-center py-20 text-sm text-gray-500">Loading…</div>
      ) : (
        <div className="flex flex-col gap-5">
          <div className="flex items-center gap-4">
            <Avatar name={`${data.firstName} ${data.lastName}`} src={data.profilePictureUrl} size="lg" />
            <div className="min-w-0">
              <div className="truncate text-lg font-semibold">
                {data.firstName} {data.lastName}
              </div>
              <div className="truncate text-sm text-gray-500">{data.email}</div>
            </div>
          </div>

          <form className="flex flex-col gap-4" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
            <div className="grid grid-cols-2 gap-3">
              <Input label="First name" {...register("firstName")} error={errors.firstName?.message} />
              <Input label="Last name" {...register("lastName")} error={errors.lastName?.message} />
            </div>
            <Input label="Academic title" placeholder="Prof. / Dr." {...register("academicTitle")} />
            <Input label="Institution" {...register("institution")} />
            <Button type="submit" disabled={!isDirty} loading={isSubmitting || mutation.isPending}>
              Save changes
            </Button>
          </form>

          <Link
            href="/profile/sns"
            className="flex items-center justify-between rounded-xl border border-gray-200 bg-white p-4 hover:bg-gray-50"
          >
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-gray-100 p-2 text-gray-600">
                <Link2 className="h-5 w-5" />
              </div>
              <div>
                <div className="text-sm font-semibold">Linked accounts</div>
                <div className="text-xs text-gray-500">Facebook / LinkedIn</div>
              </div>
            </div>
            <span className="text-sm text-brand-600">Manage</span>
          </Link>
        </div>
      )}
    </AppShell>
  );
}
