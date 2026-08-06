"use client";

import * as React from "react";
import { Toaster } from "sonner";
import { formatDate, translate, type Lang } from "@/lib/i18n";

/**
 * Language context.
 *
 * The choice persists in localStorage and is mirrored onto `<html lang>` so screen
 * readers and the browser's own translation prompt see the right language.
 */

interface LanguageContextValue {
  lang: Lang;
  setLang: (lang: Lang) => void;
  toggle: () => void;
  t: (key: string, ...args: (string | number)[]) => string;
  d: (value: string | Date | null | undefined) => string;
}

const LanguageContext = React.createContext<LanguageContextValue | null>(null);

const STORAGE_KEY = "bibliotech.lang";

export function LanguageProvider({
  children,
  initial = "fr",
}: {
  children: React.ReactNode;
  initial?: Lang;
}) {
  const [lang, setLangState] = React.useState<Lang>(initial);

  // Read the stored preference after mount. Doing this during render would make the
  // server and client markup disagree and trigger a hydration mismatch.
  React.useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored === "fr" || stored === "en") {
      setLangState(stored);
    } else if (navigator.language.startsWith("en")) {
      setLangState("en");
    }
  }, []);

  React.useEffect(() => {
    document.documentElement.lang = lang;
  }, [lang]);

  const setLang = React.useCallback((next: Lang) => {
    setLangState(next);
    window.localStorage.setItem(STORAGE_KEY, next);
  }, []);

  const value = React.useMemo<LanguageContextValue>(
    () => ({
      lang,
      setLang,
      toggle: () => setLang(lang === "fr" ? "en" : "fr"),
      t: (key, ...args) => translate(lang, key, ...args),
      d: (date) => formatDate(lang, date),
    }),
    [lang, setLang],
  );

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useI18n() {
  const context = React.useContext(LanguageContext);
  if (!context) throw new Error("useI18n must be used inside <LanguageProvider>");
  return context;
}

// ── Session ────────────────────────────────────────────────────────────────────

export interface SessionUser {
  id: number;
  nom: string;
  email: string;
  role: "Admin" | "Lecteur";
}

const SessionContext = React.createContext<{
  user: SessionUser | null;
  setUser: (user: SessionUser | null) => void;
} | null>(null);

export function SessionProvider({
  children,
  initialUser,
}: {
  children: React.ReactNode;
  initialUser: SessionUser | null;
}) {
  const [user, setUser] = React.useState(initialUser);
  const value = React.useMemo(() => ({ user, setUser }), [user]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const context = React.useContext(SessionContext);
  if (!context) throw new Error("useSession must be used inside <SessionProvider>");
  return context;
}

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <LanguageProvider>
      {children}
      <Toaster
        position="bottom-center"
        toastOptions={{
          className:
            "rounded-full border border-line-soft bg-[#1a222e] text-white shadow-(--shadow-card-hover)",
        }}
      />
    </LanguageProvider>
  );
}
