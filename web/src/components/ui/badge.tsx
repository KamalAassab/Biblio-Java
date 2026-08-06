import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold whitespace-nowrap",
  {
    variants: {
      variant: {
        neutral: "bg-surface-chip text-ink-muted",
        success: "bg-ok-soft text-[#116b3d]",
        warning: "bg-warn-soft text-[#925105]",
        danger: "bg-bad-soft text-[#a32c24]",
        info: "bg-navy-100 text-navy-600",
        gold: "bg-gold-100 text-gold-700",
      },
      dot: { true: "", false: "" },
    },
    defaultVariants: { variant: "neutral", dot: false },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, dot, children, ...props }: BadgeProps) {
  return (
    <span className={cn(badgeVariants({ variant, dot }), className)} {...props}>
      {dot && <span className="size-1.5 rounded-full bg-current" aria-hidden />}
      {children}
    </span>
  );
}
