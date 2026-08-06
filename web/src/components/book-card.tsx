"use client";

import { motion } from "framer-motion";
import { BookCover } from "@/components/book-cover";
import { Badge } from "@/components/ui/badge";
import { useI18n } from "@/components/providers";
import type { Book } from "@/lib/queries";

/**
 * A catalogue tile.
 *
 * The title block reserves two lines whether or not it needs them, so authors and
 * availability badges land on the same baseline across every card in the grid.
 */
export function BookCard({
  book,
  onOpen,
  index = 0,
}: {
  book: Book;
  onOpen: (book: Book) => void;
  index?: number;
}) {
  const { t } = useI18n();

  return (
    <motion.button
      type="button"
      onClick={() => onOpen(book)}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      // Stagger capped so a large catalogue does not take seconds to finish appearing.
      transition={{ duration: 0.35, delay: Math.min(index * 0.03, 0.4), ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -4 }}
      className="group flex w-full flex-col rounded-[--radius-lg] border border-line-soft bg-surface p-4 text-left
                 shadow-[--shadow-card] transition-shadow duration-300 hover:shadow-[--shadow-card-hover]
                 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-navy-600"
      aria-label={`${book.titre} — ${book.auteur}`}
    >
      <BookCover title={book.titre} author={book.auteur} genre={book.genre} />

      <div className="mt-4 flex min-h-[2.6rem] items-start">
        <h3 className="line-clamp-2 text-sm font-bold leading-snug text-ink">{book.titre}</h3>
      </div>

      <p className="mt-1 truncate text-xs text-ink-muted">{book.auteur}</p>

      <div className="mt-4 flex items-center justify-between gap-2">
        <Badge variant={book.disponibilite ? "success" : "warning"} dot>
          {t(book.disponibilite ? "book.available" : "book.borrowed")}
        </Badge>
        {book.genre && (
          <span className="truncate text-[11px] text-ink-faint" title={book.genre}>
            {book.genre}
          </span>
        )}
      </div>
    </motion.button>
  );
}
