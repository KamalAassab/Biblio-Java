"use client";

import * as React from "react";
import { motion } from "framer-motion";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * The oversized page title that opens every view, with optional actions on the right.
 * Wraps below the title on narrow screens rather than crushing either side.
 */
export function PageHeader({
  title,
  subtitle,
  actions,
}: {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
}) {
  return (
    <header className="flex flex-col gap-4 pb-6 sm:flex-row sm:items-end sm:justify-between">
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
        className="min-w-0"
      >
        <h1 className="text-[clamp(1.9rem,4.2vw,2.75rem)] font-black leading-[1.05] tracking-[-0.02em] text-ink">
          {title}
        </h1>
        {subtitle && <p className="mt-2 text-sm text-ink-muted">{subtitle}</p>}
      </motion.div>

      {actions && <div className="flex shrink-0 flex-wrap items-center gap-2.5">{actions}</div>}
    </header>
  );
}

/** A section title with an optional trailing action. */
export function SectionHeading({
  title,
  action,
  className,
}: {
  title: string;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex items-center justify-between gap-4 pb-3", className)}>
      <h2 className="text-lg font-bold tracking-tight text-ink">{title}</h2>
      {action}
    </div>
  );
}

/**
 * Placeholder for an empty list.
 *
 * Takes both strings from the caller so "nothing matched your filters" and "nothing
 * exists yet" can say different things — they need different next steps.
 */
export function EmptyState({
  icon: Icon,
  title,
  message,
  action,
}: {
  icon: LucideIcon;
  title: string;
  message: string;
  action?: React.ReactNode;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.97 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
      className="flex flex-col items-center justify-center gap-3 px-6 py-16 text-center"
    >
      <div className="flex size-20 items-center justify-center rounded-full bg-surface-chip">
        <Icon className="size-8 text-ink-faint" aria-hidden />
      </div>
      <h3 className="mt-2 text-base font-bold text-ink">{title}</h3>
      <p className="max-w-sm text-sm text-ink-muted">{message}</p>
      {action && <div className="mt-3">{action}</div>}
    </motion.div>
  );
}

/**
 * A dashboard metric.
 *
 * The figure counts up when it changes rather than snapping, which draws the eye to
 * what actually moved after a refresh.
 */
export function StatCard({
  label,
  value,
  caption,
  icon: Icon,
  tone = "navy",
  index = 0,
}: {
  label: string;
  value: number;
  caption?: string | null;
  icon: LucideIcon;
  tone?: "navy" | "ok" | "warn" | "gold";
  index?: number;
}) {
  const shown = useCountUp(value);

  const tones = {
    navy: "bg-navy-100 text-navy-600",
    ok: "bg-ok-soft text-ok",
    warn: "bg-warn-soft text-warn",
    gold: "bg-gold-100 text-gold-700",
  } as const;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.06, ease: [0.22, 1, 0.36, 1] }}
      className="rounded-(--radius-lg) border border-line-soft bg-surface p-3.5 sm:p-6 shadow-(--shadow-card)"
    >
      <div className="flex items-start justify-between gap-2 sm:gap-4">
        <p className="text-2xl sm:text-4xl font-extrabold leading-none tracking-tight text-ink">{shown}</p>
        <span className={cn("flex size-9 sm:size-11 items-center justify-center rounded-(--radius-sm)", tones[tone])}>
          <Icon className="size-4 sm:size-5" aria-hidden />
        </span>
      </div>
      <p className="mt-2 sm:mt-3 text-xs sm:text-sm font-medium text-ink-muted">{label}</p>
      {caption && <p className={cn("mt-1 text-xs font-medium", tones[tone].split(" ")[1])}>{caption}</p>}
    </motion.div>
  );
}

/** Eases a displayed number towards its target over ~600ms. */
function useCountUp(target: number): number {
  const [shown, setShown] = React.useState(0);
  const frame = React.useRef<number | null>(null);

  React.useEffect(() => {
    // Honour a reduced-motion preference by jumping straight to the value.
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      setShown(target);
      return;
    }

    const start = performance.now();
    const from = shown;
    const duration = 600;

    const step = (now: number) => {
      const progress = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      setShown(Math.round(from + (target - from) * eased));
      if (progress < 1) frame.current = requestAnimationFrame(step);
    };

    frame.current = requestAnimationFrame(step);
    return () => {
      if (frame.current !== null) cancelAnimationFrame(frame.current);
    };
    // `shown` is intentionally excluded: including it would restart the animation
    // on every frame it sets.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target]);

  return shown;
}

/** A shortcut tile linking to another page or opening a dialog. */
export function ActionCard({
  icon: Icon,
  title,
  subtitle,
  tone = "navy",
  onClick,
  index = 0,
}: {
  icon: LucideIcon;
  title: string;
  subtitle: string;
  tone?: "navy" | "ok" | "warn" | "gold";
  onClick: () => void;
  index?: number;
}) {
  const tones = {
    navy: "bg-navy-100 text-navy-600",
    ok: "bg-ok-soft text-ok",
    warn: "bg-warn-soft text-warn",
    gold: "bg-gold-100 text-gold-700",
  } as const;

  return (
    <motion.button
      type="button"
      onClick={onClick}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: 0.1 + index * 0.06, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -3 }}
      className="group rounded-(--radius-lg) border border-line-soft bg-surface p-5 text-left
                 shadow-(--shadow-card) transition-shadow duration-300 hover:shadow-(--shadow-card-hover)
                 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-600"
    >
      <span className={cn("flex size-11 items-center justify-center rounded-(--radius-sm)", tones[tone])}>
        <Icon className="size-5" aria-hidden />
      </span>
      <p className="mt-4 text-sm font-bold text-ink">{title}</p>
      <p className="mt-0.5 text-xs text-ink-muted">{subtitle}</p>
    </motion.button>
  );
}
