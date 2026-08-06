"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
  icon?: React.ReactNode;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, invalid, icon, ...props }, ref) => {
    const field = (
      <input
        type={type}
        ref={ref}
        aria-invalid={invalid || undefined}
        className={cn(
          "h-13 w-full rounded-(--radius-sm) border bg-surface-sunk px-4 text-sm text-ink",
          "transition-colors duration-200 placeholder:text-ink-faint",
          "focus:border-navy-600 focus:bg-surface focus:outline-none",
          "disabled:cursor-not-allowed disabled:opacity-60",
          invalid ? "border-bad bg-bad-soft" : "border-line",
          icon && "pl-11",
          className,
        )}
        {...props}
      />
    );

    if (!icon) return field;
    return (
      <div className="relative">
        <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-ink-muted [&_svg]:size-[18px]">
          {icon}
        </span>
        {field}
      </div>
    );
  },
);
Input.displayName = "Input";

export const Textarea = React.forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }
>(({ className, invalid, ...props }, ref) => (
  <textarea
    ref={ref}
    aria-invalid={invalid || undefined}
    className={cn(
      "min-h-28 w-full rounded-(--radius-sm) border bg-surface-sunk p-4 text-sm text-ink",
      "transition-colors duration-200 placeholder:text-ink-faint",
      "focus:border-navy-600 focus:bg-surface focus:outline-none",
      invalid ? "border-bad bg-bad-soft" : "border-line",
      className,
    )}
    {...props}
  />
));
Textarea.displayName = "Textarea";

/** A password field with a reveal toggle. */
export const PasswordInput = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, ...props }, ref) => {
    const [revealed, setRevealed] = React.useState(false);
    return (
      <div className="relative">
        <Input
          ref={ref}
          type={revealed ? "text" : "password"}
          className={cn("pr-12", className)}
          {...props}
        />
        <button
          type="button"
          onClick={() => setRevealed((value) => !value)}
          className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full p-2 text-ink-muted transition-colors hover:text-navy-600"
          aria-label={revealed ? "Hide password" : "Show password"}
          tabIndex={-1}
        >
          {revealed ? <EyeOff /> : <Eye />}
        </button>
      </div>
    );
  },
);
PasswordInput.displayName = "PasswordInput";

function Eye() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="size-[18px]">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function EyeOff() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="size-[18px]">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="12" cy="12" r="3" />
      <path d="m3 3 18 18" strokeLinecap="round" />
    </svg>
  );
}
