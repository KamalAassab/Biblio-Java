"use client";

import * as React from "react";
import * as LabelPrimitive from "@radix-ui/react-label";
import * as SwitchPrimitive from "@radix-ui/react-switch";
import { cn, hueFor, initialsOf } from "@/lib/utils";

export const Label = React.forwardRef<
  React.ElementRef<typeof LabelPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof LabelPrimitive.Root>
>(({ className, ...props }, ref) => (
  <LabelPrimitive.Root
    ref={ref}
    className={cn("text-xs font-semibold text-ink-soft", className)}
    {...props}
  />
));
Label.displayName = "Label";

export const Switch = React.forwardRef<
  React.ElementRef<typeof SwitchPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof SwitchPrimitive.Root>
>(({ className, ...props }, ref) => (
  <SwitchPrimitive.Root
    ref={ref}
    className={cn(
      "peer inline-flex h-8 w-14 shrink-0 cursor-pointer items-center rounded-full",
      "border-2 border-transparent transition-colors duration-200",
      "data-[state=checked]:bg-ok data-[state=unchecked]:bg-line",
      "disabled:cursor-not-allowed disabled:opacity-50",
      className,
    )}
    {...props}
  >
    <SwitchPrimitive.Thumb
      className={cn(
        "pointer-events-none block size-6 rounded-full bg-white shadow-sm ring-0",
        "transition-transform duration-200 data-[state=checked]:translate-x-6 data-[state=unchecked]:translate-x-0.5",
      )}
    />
  </SwitchPrimitive.Root>
));
Switch.displayName = "Switch";

/**
 * Initials badge.
 *
 * Colour is derived from the initials, so the same person is always the same colour
 * without storing a preference — in a members table, a column of identical badges
 * would carry no information.
 */
export function Avatar({
  name,
  size = 40,
  className,
  ring = false,
}: {
  name: string;
  size?: number;
  className?: string;
  ring?: boolean;
}) {
  const initials = initialsOf(name);
  const hue = hueFor(initials);
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-full font-bold text-white",
        ring && "ring-2 ring-white",
        className,
      )}
      style={{
        width: size,
        height: size,
        fontSize: Math.max(11, size * 0.36),
        backgroundImage: `linear-gradient(135deg, hsl(${(hue + 18) % 360} 62% 62%), hsl(${hue} 55% 46%))`,
      }}
      aria-hidden
    >
      {initials}
    </span>
  );
}

export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("animate-pulse rounded-(--radius-sm) bg-surface-chip", className)}
      {...props}
    />
  );
}

export function Separator({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div role="separator" className={cn("h-px w-full bg-line-soft", className)} {...props} />;
}
