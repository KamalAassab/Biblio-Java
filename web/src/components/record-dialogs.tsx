"use client";

import * as React from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/misc";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useI18n } from "@/components/providers";
import { ApiError, apiGet, apiSend } from "@/lib/client";
import type { Book, Member } from "@/lib/queries";

/** The library's standard lending period. */
const DEFAULT_LOAN_DAYS = 20;

function isoInDays(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}

/** Records a new loan: a reader, an available book, and a due date. */
export function LoanDialog({
  open,
  onOpenChange,
  onDone,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDone: () => void;
}) {
  const { t } = useI18n();
  const [readers, setReaders] = React.useState<Member[]>([]);
  const [books, setBooks] = React.useState<Book[]>([]);
  const [reader, setReader] = React.useState("");
  const [book, setBook] = React.useState("");
  const [dueDate, setDueDate] = React.useState(isoInDays(DEFAULT_LOAN_DAYS));
  const [pending, setPending] = React.useState(false);
  const [loading, setLoading] = React.useState(false);

  React.useEffect(() => {
    if (!open) return;
    setReader("");
    setBook("");
    setDueDate(isoInDays(DEFAULT_LOAN_DAYS));
    setLoading(true);

    Promise.all([
      apiGet<{ readers: Member[] }>("/api/emprunts"),
      apiGet<{ books: Book[] }>("/api/livres"),
    ])
      .then(([loans, catalogue]) => {
        setReaders(loans.readers);
        // Only books currently on the shelf can be lent.
        setBooks(catalogue.books.filter((entry) => entry.disponibilite));
      })
      .catch((caught) => toast.error(t((caught as ApiError).key ?? "toast.error")))
      .finally(() => setLoading(false));
  }, [open, t]);

  async function submit() {
    if (!reader) return toast.error(t("error.reader.required"));
    if (!book) return toast.error(t("error.book.required"));

    setPending(true);
    try {
      await apiSend("/api/emprunts", "POST", {
        idUtilisateur: Number(reader),
        idLivre: Number(book),
        dateRetour: dueDate,
      });
      toast.success(t("toast.loan.created"));
      onOpenChange(false);
      onDone();
    } catch (caught) {
      toast.error(t((caught as ApiError).key ?? "toast.error"));
    } finally {
      setPending(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("loan.new")}</DialogTitle>
          <DialogDescription>{t("loan.new.sub")}</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label>{t("loan.reader")}</Label>
            <Select value={reader} onValueChange={setReader} disabled={loading}>
              <SelectTrigger>
                <SelectValue placeholder={loading ? t("action.loading") : t("action.select")} />
              </SelectTrigger>
              <SelectContent>
                {readers.map((entry) => (
                  <SelectItem key={entry.id} value={String(entry.id)}>
                    {entry.nom}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-2">
            <Label>{t("loan.book")}</Label>
            <Select value={book} onValueChange={setBook} disabled={loading}>
              <SelectTrigger>
                <SelectValue placeholder={loading ? t("action.loading") : t("action.select")} />
              </SelectTrigger>
              <SelectContent>
                {books.map((entry) => (
                  <SelectItem key={entry.id} value={String(entry.id)}>
                    {entry.titre} — {entry.auteur}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="dueDate">{t("loan.dueOn")}</Label>
            <Input
              id="dueDate"
              type="date"
              value={dueDate}
              min={isoInDays(0)}
              onChange={(event) => setDueDate(event.target.value)}
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={pending}>
            {t("action.cancel")}
          </Button>
          <Button onClick={submit} loading={pending}>
            {t("action.save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/** Records a reservation on behalf of a reader. */
export function ReservationDialog({
  open,
  onOpenChange,
  onDone,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDone: () => void;
}) {
  const { t } = useI18n();
  const [readers, setReaders] = React.useState<Member[]>([]);
  const [reader, setReader] = React.useState("");
  const [date, setDate] = React.useState(isoInDays(0));
  const [pending, setPending] = React.useState(false);
  const [loading, setLoading] = React.useState(false);

  React.useEffect(() => {
    if (!open) return;
    setReader("");
    setDate(isoInDays(0));
    setLoading(true);

    apiGet<{ readers: Member[] }>("/api/reservations")
      .then((data) => setReaders(data.readers))
      .catch((caught) => toast.error(t((caught as ApiError).key ?? "toast.error")))
      .finally(() => setLoading(false));
  }, [open, t]);

  async function submit() {
    if (!reader) return toast.error(t("error.reader.required"));

    setPending(true);
    try {
      await apiSend("/api/reservations", "POST", {
        idUtilisateur: Number(reader),
        dateReservation: date,
      });
      toast.success(t("toast.res.created"));
      onOpenChange(false);
      onDone();
    } catch (caught) {
      toast.error(t((caught as ApiError).key ?? "toast.error"));
    } finally {
      setPending(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("res.new")}</DialogTitle>
          <DialogDescription>{t("res.new.sub")}</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label>{t("res.reader")}</Label>
            <Select value={reader} onValueChange={setReader} disabled={loading}>
              <SelectTrigger>
                <SelectValue placeholder={loading ? t("action.loading") : t("action.select")} />
              </SelectTrigger>
              <SelectContent>
                {readers.map((entry) => (
                  <SelectItem key={entry.id} value={String(entry.id)}>
                    {entry.nom}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="resDate">{t("res.date")}</Label>
            <Input
              id="resDate"
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="secondary" onClick={() => onOpenChange(false)} disabled={pending}>
            {t("action.cancel")}
          </Button>
          <Button onClick={submit} loading={pending}>
            {t("action.save")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
