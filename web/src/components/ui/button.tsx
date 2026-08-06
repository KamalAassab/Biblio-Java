"use client";

import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full text-sm font-semibold " +
    "transition-all duration-200 disabled:pointer-events-none disabled:opacity-55 " +
    "[&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 active:translate-y-px",
  {
    variants: {
      variant: {
        primary:
          "bg-navy-600 text-white shadow-[0_2px_8px_-2px_rgb(0_64_128/0.5)] hover:bg-navy-500 " +
          "hover:shadow-[0_6px_16px_-4px_rgb(0_64_128/0.55)]",
        accent:
          "bg-gold-500 text-navy-900 shadow-[0_2px_8px_-2px_rgb(233_164_0/0.5)] hover:bg-gold-400",
        secondary:
          "border border-line bg-surface text-ink hover:border-navy-200 hover:bg-surface-sunk",
        ghost: "text-ink-soft hover:bg-surface-chip hover:text-ink",
        danger: "bg-bad text-white hover:bg-bad/90 shadow-[0_2px_8px_-2px_rgb(224_74_63/0.5)]",
        link: "text-navy-600 underline-offset-4 hover:underline",
      },
      size: {
        sm: "h-9 px-4 text-xs",
        md: "h-11 px-5",
        lg: "h-13 px-7 text-base",
        icon: "size-11",
        "icon-sm": "size-9",
      },
    },
    defaultVariants: { variant: "primary", size: "md" },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  loading?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, loading = false, children, disabled, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    return (
      <Comp
        ref={ref}
        className={cn(buttonVariants({ variant, size, className }))}
        disabled={disabled || loading}
        {...props}
      >
        {loading ? (
          <>
            <Loader2 className="animate-spin" aria-hidden />
            <span className="sr-only">Loading</span>
            {children}
          </>
        ) : (
          children
        )}
      </Comp>
    );
  },
);
Button.displayName = "Button";

export { buttonVariants };
