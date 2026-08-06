"use client";

import * as React from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import { motion } from "framer-motion";
import { Globe, Lock, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input, PasswordInput } from "@/components/ui/input";
import { Label } from "@/components/ui/misc";
import { useI18n } from "@/components/providers";
import { ApiError, apiSend } from "@/lib/client";

/**
 * The sign-in screen: an editorial panel carrying the institutional identity beside
 * the credential form. The panel collapses on small screens, where the form is what
 * matters.
 */
export function LoginView({ demoMode }: { demoMode: boolean }) {
  const { t, lang, toggle } = useI18n();
  const router = useRouter();
  const searchParams = useSearchParams();

  const [username, setUsername] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [pending, setPending] = React.useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);

    if (!username.trim() || !password) {
      setError(t("login.error.empty"));
      return;
    }

    setPending(true);
    try {
      await apiSend("/api/auth/login", "POST", { nom: username.trim(), motDePasse: password });

      // Only accept an internal path — an attacker-supplied `?next=` must not be
      // able to bounce a freshly authenticated user to another origin.
      const next = searchParams.get("next");
      const safeNext = next && /^\/(?!\/)/.test(next) ? next : "/dashboard";

      router.replace(safeNext);
      router.refresh();
    } catch (caught) {
      const apiError = caught as ApiError;
      setError(
        apiError.key === "login.error.locked" && apiError.retryAfter
          ? t("login.error.locked", apiError.retryAfter)
          : t(apiError.key ?? "toast.error"),
      );
      setPassword("");
      setPending(false);
    }
  }

  function useDemoAccount(name: string, secret: string) {
    setUsername(name);
    setPassword(secret);
    setError(null);
  }

  return (
    <div className="flex min-h-dvh flex-col p-3 sm:p-4">
      <div className="flex justify-end pb-3">
        <button
          type="button"
          onClick={toggle}
          className="flex items-center gap-2 rounded-full bg-white/70 px-4 py-2 text-xs font-semibold text-ink-soft transition-colors hover:bg-white"
        >
          <Globe className="size-4" />
          {lang.toUpperCase()}
        </button>
      </div>

      <div className="grid flex-1 gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)]">
        {/* Editorial panel */}
        <motion.section
          initial={{ opacity: 0, x: -16 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
          className="relative hidden overflow-hidden rounded-(--radius-xl) bg-gradient-to-br from-navy-700 to-navy-600 p-10 lg:flex lg:flex-col"
        >
          <span className="absolute -right-16 -top-24 size-72 rounded-full bg-gold-500/25" />
          <span className="absolute -bottom-24 -left-20 size-72 rounded-full bg-navy-500/40" />

          <div className="relative flex items-center gap-3">
            <span className="flex size-14 items-center justify-center rounded-(--radius-sm) bg-white shadow-lg">
              <Image
                src="/fsts-logo.png"
                alt=""
                width={38}
                height={38}
                className="size-[38px] object-contain"
                priority
              />
            </span>
            <div>
              <p className="text-xl font-extrabold text-white">{t("app.name")}</p>
              <p className="text-xs text-white/70">{t("app.university.short")}</p>
            </div>
          </div>

          <div className="relative my-auto max-w-md py-10">
            <h1 className="text-[clamp(2rem,3.2vw,2.75rem)] font-black leading-[1.1] tracking-[-0.02em] text-white">
              {t("login.hero.title")}
            </h1>
            <p className="mt-5 text-sm leading-relaxed text-white/80">{t("login.hero.sub")}</p>
          </div>

          <div className="relative rounded-(--radius-md) bg-white/12 p-5 backdrop-blur-sm">
            <p className="text-[10px] font-semibold uppercase tracking-wider text-white/65">
              {t("login.demo.title")}
            </p>
            <div className="mt-3 flex flex-col gap-2">
              <DemoRow
                role={t("login.demo.admin")}
                credentials="admin / admin123"
                label={t("login.demo.fill")}
                onUse={() => useDemoAccount("admin", "admin123")}
              />
              <DemoRow
                role={t("login.demo.reader")}
                credentials="lecteur / lecteur123"
                label={t("login.demo.fill")}
                onUse={() => useDemoAccount("lecteur", "lecteur123")}
              />
            </div>
          </div>

          <p className="relative pt-6 text-[11px] text-white/55">{t("credit.builtBy")}</p>
        </motion.section>

        {/* Form */}
        <motion.section
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.45, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
          className="flex items-center justify-center rounded-(--radius-xl) bg-surface px-5 py-10 shadow-(--shadow-card) lg:bg-transparent lg:shadow-none"
        >
          <div className="w-full max-w-sm">
            <div className="mb-8 flex items-center gap-3 lg:hidden">
              <span className="flex size-12 items-center justify-center rounded-(--radius-sm) bg-white shadow-(--shadow-card)">
                <Image
                  src="/fsts-logo.png"
                  alt=""
                  width={32}
                  height={32}
                  className="size-8 object-contain"
                />
              </span>
              <div>
                <p className="text-base font-extrabold text-ink">{t("app.name")}</p>
                <p className="text-[11px] text-ink-muted">{t("app.university.short")}</p>
              </div>
            </div>

            <h2 className="text-3xl font-extrabold tracking-tight text-ink">{t("login.welcome")}</h2>
            <p className="mt-2 text-sm text-ink-muted">{t("login.subtitle")}</p>

            <form onSubmit={submit} className="mt-8 flex flex-col gap-5" noValidate>
              <div className="flex flex-col gap-2">
                <Label htmlFor="username">{t("login.username")}</Label>
                <Input
                  id="username"
                  name="username"
                  autoComplete="username"
                  autoFocus
                  icon={<User />}
                  placeholder={t("login.username.hint")}
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  invalid={Boolean(error)}
                />
              </div>

              <div className="flex flex-col gap-2">
                <Label htmlFor="password">{t("login.password")}</Label>
                <PasswordInput
                  id="password"
                  name="password"
                  autoComplete="current-password"
                  icon={<Lock />}
                  placeholder={t("login.password.hint")}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  invalid={Boolean(error)}
                />
              </div>

              {/* aria-live so the message is announced, not just displayed. */}
              <p className="min-h-5 text-xs font-medium text-bad" role="alert" aria-live="polite">
                {error ?? ""}
              </p>

              <Button type="submit" size="lg" loading={pending} className="w-full">
                {pending ? t("login.submitting") : t("login.submit")}
              </Button>
            </form>

            {demoMode && (
              <p className="mt-6 rounded-(--radius-sm) bg-gold-100 p-3 text-[11px] leading-relaxed text-gold-700">
                {t("demo.banner")}
              </p>
            )}

            <div className="mt-8 flex flex-col gap-2 lg:hidden">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-ink-faint">
                {t("login.demo.title")}
              </p>
              <div className="flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => useDemoAccount("admin", "admin123")}
                >
                  {t("login.demo.admin")}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => useDemoAccount("lecteur", "lecteur123")}
                >
                  {t("login.demo.reader")}
                </Button>
              </div>
            </div>
          </div>
        </motion.section>
      </div>
    </div>
  );
}

function DemoRow({
  role,
  credentials,
  label,
  onUse,
}: {
  role: string;
  credentials: string;
  label: string;
  onUse: () => void;
}) {
  return (
    <div className="flex items-center justify-between gap-3 text-xs">
      <span className="min-w-0 text-white/85">
        <span className="font-semibold">{role}</span>
        <span className="mx-1.5 text-white/40">—</span>
        <span className="text-white/65">{credentials}</span>
      </span>
      <button
        type="button"
        onClick={onUse}
        className="shrink-0 rounded-full bg-white/15 px-3 py-1 font-semibold text-white transition-colors hover:bg-white/25"
      >
        {label}
      </button>
    </div>
  );
}
