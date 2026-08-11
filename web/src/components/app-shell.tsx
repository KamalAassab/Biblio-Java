"use client";

import * as React from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import {
  Bell,
  BookMarked,
  Clock,
  Globe,
  Grid2x2,
  Home,
  Info,
  LogOut,
  Menu,
  User,
  Users,
  X,
  type LucideIcon,
} from "lucide-react";
import { toast } from "sonner";
import { Avatar, Separator } from "@/components/ui/misc";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useI18n, useSession, type SessionUser } from "@/components/providers";
import { apiSend } from "@/lib/client";
import { cn } from "@/lib/utils";

const PORTFOLIO = "https://kamal-aassab.vercel.app/";

interface NavEntry {
  href: string;
  labelKey: string;
  icon: LucideIcon;
  adminOnly?: boolean;
}

const PRIMARY_NAV: NavEntry[] = [
  { href: "/dashboard", labelKey: "nav.dashboard", icon: Home },
  { href: "/catalogue", labelKey: "nav.catalogue", icon: Grid2x2 },
  { href: "/emprunts", labelKey: "nav.emprunts", icon: Clock },
  { href: "/reservations", labelKey: "nav.reservations", icon: BookMarked },
  { href: "/utilisateurs", labelKey: "nav.utilisateurs", icon: Users, adminOnly: true },
];

export function AppShell({
  user,
  overdue = 0,
  children,
}: {
  user: SessionUser;
  overdue?: number;
  children: React.ReactNode;
}) {
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const pathname = usePathname();

  // Close the drawer whenever navigation happens, or it stays open over the new page.
  React.useEffect(() => setMobileOpen(false), [pathname]);

  return (
    <div className="flex min-h-dvh gap-4 p-3 sm:p-4">
      {/* Desktop sidebar */}
      <aside className="hidden w-64 shrink-0 lg:block">
        <div className="sticky top-4 flex h-[calc(100dvh-2rem)] flex-col rounded-(--radius-xl) border border-line-soft bg-surface p-4 shadow-(--shadow-card)">
          <SidebarBody user={user} />
        </div>
      </aside>

      {/* Mobile drawer */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setMobileOpen(false)}
              className="fixed inset-0 z-40 bg-navy-900/35 backdrop-blur-sm lg:hidden"
            />
            <motion.aside
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ type: "spring", damping: 30, stiffness: 300 }}
              className="fixed inset-y-3 left-3 z-50 flex w-[17rem] flex-col rounded-(--radius-xl) border border-line-soft bg-surface p-4 shadow-(--shadow-card-hover) lg:hidden"
            >
              <button
                onClick={() => setMobileOpen(false)}
                className="absolute right-4 top-4 rounded-full p-2 text-ink-muted hover:bg-surface-chip"
                aria-label="Close menu"
              >
                <X className="size-4" />
              </button>
              <SidebarBody user={user} />
            </motion.aside>
          </>
        )}
      </AnimatePresence>

      <div className="flex min-w-0 flex-1 flex-col">
        <TopBar user={user} overdue={overdue} onOpenMenu={() => setMobileOpen(true)} />
        <main className="min-w-0 flex-1 px-1 pb-8 pt-2 sm:px-3">{children}</main>
      </div>
    </div>
  );
}

function SidebarBody({ user }: { user: SessionUser }) {
  const { t } = useI18n();
  const pathname = usePathname();
  const isAdmin = user.role === "Admin";

  return (
    <>
      <Link href="/dashboard" className="flex items-center gap-3 px-2 py-2">
        {/* The crest is a transparent PNG on a light sidebar, so it needs no plate
            or border — it reads at full size directly on the surface. */}
        <Image
          src="/fsts-logo.png"
          alt=""
          width={52}
          height={52}
          className="size-13 shrink-0 object-contain"
          priority
        />
        <span className="min-w-0">
          <span className="block truncate text-lg font-extrabold tracking-tight text-ink">
            {t("app.name")}
          </span>
          <span className="block truncate text-[11px] text-ink-muted">
            {t("app.university.short")}
          </span>
        </span>
      </Link>

      <nav className="mt-4 flex min-h-0 flex-1 flex-col overflow-y-auto scroll-slim">
        <p className="px-3 pb-1 pt-3 text-[11px] font-semibold uppercase tracking-wider text-ink-faint">
          {t("nav.menu.primary")}
        </p>
        {PRIMARY_NAV.filter((entry) => !entry.adminOnly || isAdmin).map((entry) => (
          <NavItem
            key={entry.href}
            href={entry.href}
            icon={entry.icon}
            label={t(entry.labelKey)}
            active={pathname === entry.href || pathname.startsWith(`${entry.href}/`)}
          />
        ))}

        <Separator className="my-3" />

        <p className="px-3 pb-1 text-[11px] font-semibold uppercase tracking-wider text-ink-faint">
          {t("nav.menu.account")}
        </p>
        <NavItem
          href="/profile"
          icon={User}
          label={t("nav.profile")}
          active={pathname === "/profile"}
        />
        <LanguageNavItem />
        <AboutNavItem />
      </nav>

      <div className="pt-3">
        <CreditCard />
        <SignOutButton />
      </div>
    </>
  );
}

/**
 * A sidebar row: a rounded icon chip followed by a label.
 *
 * Selection is carried by the chip alone — it fills with academic gold and the icon
 * flips to white. A full-width pill behind every active row would turn a six-item
 * sidebar into a stack of competing blocks.
 */
function NavItem({
  href,
  icon: Icon,
  label,
  active,
  onClick,
  destructive,
}: {
  href?: string;
  icon: LucideIcon;
  label: string;
  active?: boolean;
  onClick?: () => void;
  destructive?: boolean;
}) {
  const content = (
    <>
      <span
        className={cn(
          "flex size-9 shrink-0 items-center justify-center rounded-(--radius-xs) transition-colors duration-200",
          active
            ? "bg-gold-500 text-white shadow-[0_2px_8px_-2px_rgb(233_164_0/0.6)]"
            : destructive
              ? "bg-surface-chip text-ink-muted group-hover:bg-bad-soft group-hover:text-bad"
              : "bg-surface-chip text-ink-muted group-hover:bg-navy-100 group-hover:text-navy-600",
        )}
      >
        <Icon className="size-[18px]" aria-hidden />
      </span>
      <span
        className={cn(
          "truncate text-sm transition-colors duration-200",
          active
            ? "font-bold text-ink"
            : destructive
              ? "font-medium text-ink-muted group-hover:text-bad"
              : "font-medium text-ink-muted group-hover:text-ink",
        )}
      >
        {label}
      </span>
    </>
  );

  const className = cn(
    "group flex w-full items-center gap-3 rounded-(--radius-sm) px-2 py-2 text-left transition-colors",
    !active && "hover:bg-surface-sunk",
  );

  if (href) {
    return (
      <Link href={href} className={className} aria-current={active ? "page" : undefined}>
        {content}
      </Link>
    );
  }
  return (
    <button type="button" onClick={onClick} className={className}>
      {content}
    </button>
  );
}

function LanguageNavItem() {
  const { t, lang, toggle } = useI18n();
  return (
    <NavItem
      icon={Globe}
      label={`${t("action.language")} · ${lang.toUpperCase()}`}
      onClick={() => {
        toggle();
        toast.success(t("toast.language", lang === "fr" ? "English" : "Français"));
      }}
    />
  );
}

function AboutNavItem() {
  const { t } = useI18n();
  const [open, setOpen] = React.useState(false);

  return (
    <>
      <NavItem icon={Info} label={t("credit.about")} onClick={() => setOpen(true)} />
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("app.name")}</DialogTitle>
            <DialogDescription>{t("app.tagline")}</DialogDescription>
          </DialogHeader>
          <p className="text-sm leading-relaxed text-ink-soft">{t("credit.about.body")}</p>
          <div className="flex flex-col gap-2 text-sm">
            <p className="text-ink-soft">{t("app.university")}</p>
            <p className="text-ink-soft">{t("credit.builtBy")}</p>
            <a
              href={PORTFOLIO}
              target="_blank"
              rel="noopener noreferrer"
              className="font-semibold text-navy-600 hover:underline"
            >
              {t("credit.portfolio")}
            </a>
            <p className="text-xs text-ink-muted">{t("credit.desktop")}</p>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}

/** The authorship card. This is a student project and the credit is part of it. */
function CreditCard() {
  const { t } = useI18n();
  return (
    <a
      href={PORTFOLIO}
      target="_blank"
      rel="noopener noreferrer"
      className="group relative block overflow-hidden rounded-(--radius-md) bg-gradient-to-br from-navy-700 to-navy-600 p-4 transition-transform duration-300 hover:-translate-y-0.5"
    >
      <span className="absolute -right-6 -top-8 size-20 rounded-full bg-gold-500/30 transition-transform duration-500 group-hover:scale-125" />
      <span className="relative block text-[10px] font-semibold uppercase tracking-wider text-white/65">
        {t("credit.eyebrow")}
      </span>
      <span className="relative mt-1 block text-sm font-bold text-white">Kamal Aassab</span>
      <span className="relative mt-0.5 block text-[11px] text-white/75">{t("credit.portfolio")}</span>
    </a>
  );
}

function SignOutButton() {
  const { t } = useI18n();
  const router = useRouter();
  const [pending, setPending] = React.useState(false);

  async function signOut() {
    setPending(true);
    try {
      await apiSend("/api/auth/logout", "POST");
    } catch {
      // Signing out must succeed from the user's point of view even if the request
      // fails — the redirect below drops them back at the login screen either way.
    }
    router.replace("/login");
    router.refresh();
  }

  return (
    <div className="mt-2">
      <NavItem
        icon={LogOut}
        label={pending ? t("action.loading") : t("nav.logout")}
        onClick={signOut}
        destructive
      />
    </div>
  );
}

function TopBar({
  user,
  overdue,
  onOpenMenu,
}: {
  user: SessionUser;
  overdue: number;
  onOpenMenu: () => void;
}) {
  const { t } = useI18n();
  const router = useRouter();
  const { setUser } = useSession();

  async function signOut() {
    try {
      await apiSend("/api/auth/logout", "POST");
    } catch {
      /* see SignOutButton */
    }
    setUser(null);
    router.replace("/login");
    router.refresh();
  }

  return (
    <header className="flex items-center justify-between gap-3 px-1 py-3 sm:px-3">
      <Button
        variant="ghost"
        size="icon"
        onClick={onOpenMenu}
        className="lg:hidden rounded-(--radius-md) border border-line-soft bg-surface shadow-(--shadow-card) text-ink-soft hover:text-ink hover:bg-surface-chip"
        aria-label="Open menu"
      >
        <Menu className="size-5" />
      </Button>

      <div className="ml-auto flex items-center gap-2">
        <Link
          href="/emprunts"
          className="relative flex size-11 items-center justify-center rounded-full bg-white/70 text-ink-soft transition-colors hover:bg-white"
          aria-label={overdue > 0 ? `${t("dash.stat.overdue")}: ${overdue}` : t("nav.emprunts")}
        >
          <Bell className="size-5" />
          {overdue > 0 && (
            <span className="absolute right-1.5 top-1.5 flex min-w-4 items-center justify-center rounded-full bg-bad px-1 text-[10px] font-bold text-white ring-2 ring-canvas">
              {overdue > 9 ? "9+" : overdue}
            </span>
          )}
        </Link>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="flex items-center gap-2.5 rounded-full py-1.5 pl-1.5 pr-3.5 transition-colors hover:bg-white">
              <Avatar name={user.nom} size={38} />
              <span className="hidden min-w-0 text-left sm:block">
                <span className="block truncate text-sm font-bold text-ink">{user.nom}</span>
                <span className="block truncate text-[11px] text-ink-muted">
                  {t(user.role === "Admin" ? "user.role.admin" : "user.role.reader")}
                </span>
              </span>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>{user.email || user.nom}</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link href="/profile">
                <User />
                {t("nav.profile")}
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem destructive onSelect={signOut}>
              <LogOut />
              {t("nav.logout")}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
