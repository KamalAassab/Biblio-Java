"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { AnimatePresence } from "framer-motion";
import { BookOpen, Plus, RefreshCw, Search } from "lucide-react";
import { BookCard } from "@/components/book-card";
import { BookDialog } from "@/components/book-dialog";
import { EmptyState, PageHeader } from "@/components/page-furniture";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useI18n } from "@/components/providers";
import { cn } from "@/lib/utils";
import type { Book } from "@/lib/queries";

type Availability = "all" | "available" | "borrowed";

const ALL_CATEGORIES = "__all__";

export function CatalogueView({
  books,
  genres,
  isAdmin,
}: {
  books: Book[];
  genres: string[];
  isAdmin: boolean;
}) {
  const { t } = useI18n();
  const router = useRouter();

  const [query, setQuery] = React.useState("");
  const [category, setCategory] = React.useState<string>(ALL_CATEGORIES);
  const [availability, setAvailability] = React.useState<Availability>("all");

  const [viewing, setViewing] = React.useState<Book | null>(null);
  const [creating, setCreating] = React.useState(false);

  const refresh = React.useCallback(() => router.refresh(), [router]);

  // Filtering runs client-side: the catalogue is small enough that a round trip per
  // keystroke would feel slower than it does now, and every field is already loaded.
  const results = React.useMemo(() => {
    const needle = query.trim().toLowerCase();
    return books.filter((book) => {
      if (availability !== "all") {
        const wantAvailable = availability === "available";
        if (book.disponibilite !== wantAvailable) return false;
      }
      if (category !== ALL_CATEGORIES && (book.genre ?? "").toLowerCase() !== category.toLowerCase()) {
        return false;
      }
      if (!needle) return true;
      return `${book.titre} ${book.auteur} ${book.genre ?? ""}`.toLowerCase().includes(needle);
    });
  }, [books, query, category, availability]);

  const filtering = query.trim() !== "" || category !== ALL_CATEGORIES || availability !== "all";

  return (
    <>
      <PageHeader
        title={t("page.catalogue.title")}
        subtitle={t("page.catalogue.sub")}
        actions={
          <>
            {isAdmin && (
              <Button onClick={() => setCreating(true)}>
                <Plus />
                {t("cat.add")}
              </Button>
            )}
            <Button
              variant="secondary"
              size="icon"
              onClick={refresh}
              aria-label={t("action.refresh")}
              title={t("action.refresh")}
            >
              <RefreshCw />
            </Button>
          </>
        }
      />

      {/* Search bar: category selector, query field and submit, in one pill. */}
      <div className="flex flex-col gap-3 rounded-[--radius-xl] border border-line-soft bg-surface p-2 shadow-[--shadow-card] sm:flex-row sm:items-center sm:rounded-full sm:p-2">
        <div className="sm:w-52">
          <Select value={category} onValueChange={setCategory}>
            <SelectTrigger className="h-12 rounded-full border-0 bg-transparent sm:border-r sm:border-line-soft">
              <SelectValue placeholder={t("cat.allCategories")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL_CATEGORIES}>{t("cat.allCategories")}</SelectItem>
              {genres.map((genre) => (
                <SelectItem key={genre} value={genre}>
                  {genre}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-4 top-1/2 size-[18px] -translate-y-1/2 text-ink-muted" />
          <input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t("cat.search")}
            aria-label={t("action.search")}
            maxLength={120}
            className="h-12 w-full rounded-full bg-transparent pl-12 pr-4 text-sm text-ink placeholder:text-ink-faint focus:outline-none"
          />
        </div>

        <span className="hidden shrink-0 pr-2 text-xs font-medium text-ink-muted sm:block">
          {t("cat.count", results.length)}
        </span>
      </div>

      {/* Availability filters */}
      <div className="flex flex-wrap gap-2 pt-4">
        {(["all", "available", "borrowed"] as const).map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setAvailability(value)}
            className={cn(
              "rounded-full px-4 py-2 text-xs font-semibold transition-colors",
              availability === value
                ? "bg-navy-600 text-white"
                : "bg-surface-chip text-ink-muted hover:bg-navy-100 hover:text-navy-600",
            )}
          >
            {t(`cat.filter.${value}`)}
          </button>
        ))}
      </div>

      {results.length === 0 ? (
        <div className="mt-6 rounded-[--radius-lg] border border-line-soft bg-surface shadow-[--shadow-card]">
          <EmptyState
            icon={filtering ? Search : BookOpen}
            title={t(filtering ? "cat.empty.title" : "cat.empty.none")}
            message={t(filtering ? "cat.empty.sub" : "cat.empty.none.sub")}
            action={
              filtering ? (
                <Button
                  variant="secondary"
                  onClick={() => {
                    setQuery("");
                    setCategory(ALL_CATEGORIES);
                    setAvailability("all");
                  }}
                >
                  {t("cat.filter.all")}
                </Button>
              ) : isAdmin ? (
                <Button onClick={() => setCreating(true)}>
                  <Plus />
                  {t("cat.add")}
                </Button>
              ) : null
            }
          />
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 pt-6 sm:grid-cols-3 lg:grid-cols-4 2xl:grid-cols-5">
          <AnimatePresence mode="popLayout">
            {results.map((book, index) => (
              <BookCard key={book.id} book={book} onOpen={setViewing} index={index} />
            ))}
          </AnimatePresence>
        </div>
      )}

      <BookDialog
        mode="view"
        book={viewing}
        open={viewing !== null}
        isAdmin={isAdmin}
        onOpenChange={(open) => !open && setViewing(null)}
        onDone={refresh}
      />
      <BookDialog
        mode="create"
        open={creating}
        isAdmin={isAdmin}
        onOpenChange={setCreating}
        onDone={refresh}
      />
    </>
  );
}
