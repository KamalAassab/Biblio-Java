import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Table presentation: tall rows, a quiet header, and status carried by pills.
 *
 * The wrapper scrolls horizontally on its own so a wide table never forces the whole
 * page to scroll sideways on a phone.
 */
export function Table({ className, ...props }: React.HTMLAttributes<HTMLTableElement>) {
  return (
    <div className="w-full overflow-x-auto scroll-slim">
      <table className={cn("w-full min-w-[42rem] caption-bottom text-sm", className)} {...props} />
    </div>
  );
}

export function TableHeader({ className, ...props }: React.HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={cn("bg-surface-sunk", className)} {...props} />;
}

export function TableBody({ className, ...props }: React.HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody className={cn("", className)} {...props} />;
}

export function TableRow({ className, ...props }: React.HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={cn(
        "border-b border-line-soft transition-colors last:border-0 hover:bg-surface-sunk/70",
        className,
      )}
      {...props}
    />
  );
}

export function TableHead({ className, ...props }: React.ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn(
        "h-12 px-5 text-left align-middle text-[11px] font-semibold uppercase tracking-wider text-ink-muted",
        className,
      )}
      {...props}
    />
  );
}

export function TableCell({ className, ...props }: React.TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn("h-16 px-5 align-middle text-ink-soft", className)} {...props} />;
}
