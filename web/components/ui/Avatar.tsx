"use client";

import { cn } from "@/lib/utils/cn";
import { colorFromName, initials } from "@/lib/utils/avatar";

interface UserAvatarProps {
  firstName: string;
  lastName: string;
  src?: string | null;
  size?: number;
  className?: string;
  shape?: "square" | "circle";
}

export function UserAvatar({
  firstName,
  lastName,
  src,
  size = 48,
  className,
  shape = "square"
}: UserAvatarProps) {
  const bg = colorFromName(firstName + lastName);
  return (
    <div
      className={cn(
        "relative inline-flex shrink-0 items-center justify-center overflow-hidden text-background font-serif",
        shape === "square" ? "rounded-sm" : "rounded-full",
        className
      )}
      style={{
        width: size,
        height: size,
        background: src ? undefined : bg,
        fontSize: size * 0.36
      }}
    >
      {src ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={src}
          alt={`${firstName} ${lastName}`}
          className="h-full w-full object-cover grayscale contrast-110"
        />
      ) : (
        <span className="italic">{initials(firstName, lastName)}</span>
      )}
      <span className="pointer-events-none absolute inset-0 ring-1 ring-inset ring-foreground/10" />
    </div>
  );
}

interface AvatarProps {
  name: string;
  src?: string | null;
  size?: "sm" | "md" | "lg";
  className?: string;
}

const sizeMap: Record<NonNullable<AvatarProps["size"]>, number> = {
  sm: 32,
  md: 40,
  lg: 56
};

export function Avatar({ name, src, size = "md", className }: AvatarProps) {
  const parts = name.split(" ").filter(Boolean);
  const first = parts[0] ?? "?";
  const last = parts[parts.length - 1] ?? "";
  return (
    <UserAvatar
      firstName={first}
      lastName={last}
      src={src}
      size={sizeMap[size]}
      className={className}
    />
  );
}
