"use client";

import * as React from "react";
import { Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { BookCover } from "@/components/book-cover";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input, Textarea } from "@/components/ui/input";
import { Label, Switch } from "@/components/ui/misc";
import { useI18n } from "@/components/providers";
import { ApiError, apiSend } from "@/lib/client";
import type { Book } from "@/lib/queries";

type Mode = "view" | "create" | "edit";

/**
 * One dialog serving view, create and edit.
 *
 * Keeping them together means the composed cover and the field layout cannot drift
 * apart as the form changes.
 */
export function BookDialog({
  mode: initialMode,
  book,
  open,
  isAdmin,
  onOpenChange,
  onDone,
}: {
  mode: Mode;
  book?: Book | null;
  open: boolean;
  isAdmin: boolean;
  onOpenChange: (open: boolean) => void;
  onDone: () => void;
}) {
  const { t } = useI18n();
  const [mode, setMode] = React.useState<Mode>(initialMode);
  const [pending, setPending] = React.useState(false);
  const [confirmingDelete, setConfirmingDelete] = React.useState(false);
  const [invalidField, setInvalidField] = React.useState<string | null>(null);

  const [titre, setTitre] = React.useState("");
  const [auteur, setAuteur] = React.useState("");
  const [genre, setGenre] = React.useState("");
  const [resume, setResume] = React.useState("");
  const [disponible, setDisponible] = React.useState(true);

  // Reset to the incoming record every time the dialog opens, so a cancelled edit
  // does not leak into the next one.
  React.useEffect(() => {
    if (!open) return;
    setMode(initialMode);
    setInvalidField(null);
    setTitre(book?.titre ?? "");
    setAuteur(book?.auteur ?? "");
    setGenre(book?.genre ?? "");
    setResume(book?.resume ?? "");
    setDisponible(book?.disponibilite ?? true);
  }, [open, initialMode, book]);

  async function save() {
    setPending(true);
    setInvalidField(null);
    try {
      const payload = { titre, auteur, genre, resume, disponibilite: disponible };
      if (mode === "create") {
        await apiSend("/api/livres", "POST", payload);
        toast.success(t("toast.book.added"));
      } else if (book) {
        await apiSend(`/api/livres/${book.id}`, "PUT", payload);
        toast.success(t("toast.book.updated"));
      }
      onOpenChange(false);
      onDone();
    } catch (caught) {
      const error = caught as ApiError;
      setInvalidField(error.field ?? null);
      toast.error(t(error.key ?? "toast.error"));
    } finally {
      setPending(false);
    }
  }

  async function remove() {
    if (!book) return;
    setPending(true);
    try {
      await apiSend(`/api/livres/${book.id}`, "DELETE");
      toast.success(t("toast.book.deleted"));
      setConfirmingDelete(false);
      onOpenChange(false);
      onDone();
    } catch (caught) {
      toast.error(t((caught as ApiError).key ?? "toast.error"));
    } finally {
      setPending(false);
    }
  }

  const titleKey =
    mode === "create" ? "book.add.title" : mode === "edit" ? "book.edit.title" : "book.view.title";
  const subKey = mode === "create" ? "book.add.sub" : mode === "edit" ? "book.edit.sub" : null;

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className={mode === "view" ? "max-w-2xl" : "max-w-lg"}>
          <DialogHeader>
            <DialogTitle>{t(titleKey)}</DialogTitle>
            {subKey && <DialogDescription>{t(subKey)}</DialogDescription>}
          </DialogHeader>

          {mode === "view" && book ? (
            <div className="flex flex-col gap-6 sm:flex-row">
              <div className="w-40 shrink-0 self-center sm:self-start">
                <BookCover title={book.titre} author={book.auteur} genre={book.genre} />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-xl font-bold leading-snug text-ink">{book.titre}</h3>
                <p className="mt-1 text-sm text-ink-muted">{book.auteur}</p>

                <div className="mt-4 flex flex-wrap gap-2">
                  <Badge variant={book.disponibilite ? "success" : "warning"} dot>
                    {t(book.disponibilite ? "book.available" : "book.borrowed")}
                  </Badge>
                  {book.genre && <Badge variant="neutral">{book.genre}</Badge>}
                </div>

                {book.resume && (
                  <div className="mt-5">
                    <p className="text-xs font-semibold text-ink-soft">{t("book.summary")}</p>
                    <p className="mt-1.5 whitespace-pre-line text-sm leading-relaxed text-ink-muted">
                      {book.resume}
                    </p>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              <Field label={t("book.title")} htmlFor="titre">
                <Input
                  id="titre"
                  value={titre}
                  onChange={(event) => setTitre(event.target.value)}
                  placeholder={t("book.placeholder.title")}
                  invalid={invalidField === "titre"}
                  maxLength={200}
                  autoFocus
                />
              </Field>

              <Field label={t("book.author")} htmlFor="auteur">
                <Input
                  id="auteur"
                  value={auteur}
                  onChange={(event) => setAuteur(event.target.value)}
                  placeholder={t("book.placeholder.author")}
                  invalid={invalidField === "auteur"}
                  maxLength={120}
                />
              </Field>

              <Field label={t("book.genre")} htmlFor="genre">
                <Input
                  id="genre"
                  value={genre}
                  onChange={(event) => setGenre(event.target.value)}
                  placeholder={t("book.placeholder.genre")}
                  invalid={invalidField === "genre"}
                  maxLength={60}
                />
              </Field>

              <Field label={t("book.summary")} htmlFor="resume">
                <Textarea
                  id="resume"
                  value={resume}
                  onChange={(event) => setResume(event.target.value)}
                  placeholder={t("book.placeholder.summary")}
                  invalid={invalidField === "resume"}
                  maxLength={4000}
                  rows={4}
                />
              </Field>

              <div className="flex items-center justify-between rounded-[--radius-sm] bg-surface-sunk px-4 py-3">
                <Label htmlFor="disponible" className="text-sm">
                  {t("book.available")}
                </Label>
                <Switch id="disponible" checked={disponible} onCheckedChange={setDisponible} />
              </div>
            </div>
          )}

          <DialogFooter>
            {mode === "view" ? (
              isAdmin && book ? (
                <>
                  <Button
                    variant="secondary"
                    onClick={() => setConfirmingDelete(true)}
                    disabled={pending}
                  >
                    <Trash2 />
                    {t("action.delete")}
                  </Button>
                  <Button onClick={() => setMode("edit")}>
                    <Pencil />
                    {t("action.edit")}
                  </Button>
                </>
              ) : (
                <Button onClick={() => onOpenChange(false)}>{t("action.close")}</Button>
              )
            ) : (
              <>
                <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={pending}>
                  {t("action.cancel")}
                </Button>
                <Button onClick={save} loading={pending}>
                  {t("action.save")}
                </Button>
              </>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={confirmingDelete}
        onOpenChange={setConfirmingDelete}
        title={t("confirm.delete.book.title")}
        description={t("confirm.delete.book.body", book?.titre ?? "")}
        confirmLabel={t("action.delete")}
        destructive
        pending={pending}
        onConfirm={remove}
      />
    </>
  );
}

function Field({
  label,
  htmlFor,
  children,
}: {
  label: string;
  htmlFor: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={htmlFor}>{label}</Label>
      {children}
    </div>
  );
}
