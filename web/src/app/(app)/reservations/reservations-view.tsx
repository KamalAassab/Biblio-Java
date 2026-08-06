"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { BookMarked, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { ReservationDialog } from "@/components/record-dialogs";
import { EmptyState, PageHeader } from "@/components/page-furniture";
import { Button } from "@/components/ui/button";
import { Avatar } from "@/components/ui/misc";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useI18n } from "@/components/providers";
import { ApiError, apiSend } from "@/lib/client";
import type { Reservation } from "@/lib/queries";

export function ReservationsView({
  reservations,
  isAdmin,
}: {
  reservations: Reservation[];
  isAdmin: boolean;
}) {
  const { t, d } = useI18n();
  const router = useRouter();

  const [creating, setCreating] = React.useState(false);
  const [deleting, setDeleting] = React.useState<Reservation | null>(null);
  const [pending, setPending] = React.useState(false);

  async function confirmDelete() {
    if (!deleting) return;
    setPending(true);
    try {
      await apiSend(`/api/reservations/${deleting.id}`, "DELETE");
      toast.success(t("toast.res.deleted"));
      setDeleting(null);
      router.refresh();
    } catch (caught) {
      toast.error(t((caught as ApiError).key ?? "toast.error"));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <PageHeader
        title={t("page.reservations.title")}
        subtitle={t("page.reservations.sub")}
        actions={
          isAdmin ? (
            <Button onClick={() => setCreating(true)}>
              <Plus />
              {t("res.new")}
            </Button>
          ) : null
        }
      />

      <div className="overflow-hidden rounded-(--radius-lg) border border-line-soft bg-surface shadow-(--shadow-card)">
        {reservations.length === 0 ? (
          <EmptyState
            icon={BookMarked}
            title={t("res.empty.title")}
            message={t("res.empty.sub")}
          />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-20">#</TableHead>
                <TableHead>{t("res.reader")}</TableHead>
                <TableHead>{t("res.date")}</TableHead>
                {isAdmin && <TableHead className="text-right">{""}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {reservations.map((reservation) => (
                <TableRow key={reservation.id}>
                  <TableCell className="text-ink-faint">{reservation.id}</TableCell>
                  <TableCell>
                    <span className="flex items-center gap-3">
                      <Avatar name={reservation.lecteur} size={34} />
                      <span className="font-semibold text-ink">{reservation.lecteur}</span>
                    </span>
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {d(reservation.dateReservation)}
                  </TableCell>
                  {isAdmin && (
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => setDeleting(reservation)}
                        aria-label={t("action.delete")}
                        className="text-ink-muted hover:bg-bad-soft hover:text-bad"
                      >
                        <Trash2 />
                      </Button>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <ReservationDialog
        open={creating}
        onOpenChange={setCreating}
        onDone={() => router.refresh()}
      />

      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={t("confirm.delete.res.title")}
        description={t("confirm.delete.res.body")}
        confirmLabel={t("action.delete")}
        destructive
        pending={pending}
        onConfirm={confirmDelete}
      />
    </>
  );
}
