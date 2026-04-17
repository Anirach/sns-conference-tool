"use client";

import * as SwitchPrimitive from "@radix-ui/react-switch";
import { cn } from "@/lib/utils/cn";

interface ToggleProps {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
  id?: string;
  ariaLabel?: string;
}

export function Toggle({ checked, onCheckedChange, disabled, id, ariaLabel }: ToggleProps) {
  return (
    <SwitchPrimitive.Root
      id={id}
      aria-label={ariaLabel}
      checked={checked}
      onCheckedChange={onCheckedChange}
      disabled={disabled}
      className={cn(
        "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
        "data-[state=checked]:bg-brand-500 data-[state=unchecked]:bg-surface-sunken",
        "disabled:cursor-not-allowed disabled:opacity-50",
        "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-brass-500 focus-visible:ring-offset-2"
      )}
    >
      <SwitchPrimitive.Thumb className="block h-5 w-5 translate-x-0.5 rounded-full bg-background shadow-sm transition-transform data-[state=checked]:translate-x-[22px]" />
    </SwitchPrimitive.Root>
  );
}
