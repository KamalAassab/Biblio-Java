"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Clock, Plus, Undo2 } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { LoanDialog } from "@/components/record-dialogs";
import { EmptyState, PageHeader } from "@/components/page-furniture";
import { Badge } from "@/components/ui/badge";
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
import { daysUntil } from "@/lib/utils";
import type { Loan } from "@/lib/queries";

export function LoansView({ loans, isAdmin }: { loans: Loan[]; isAdmin: boolean }) {
  const { t, d } = useI18n();
  const router = useRouter();

  const [creating, setCreating] = React.useState(false);
  const [returning, setReturning] = React.useState<Loan | null>(null);
  const [pending, setPending] = React.useState(false);

  async function confirmReturn() {
    if (!returning) return;
    setPending(true);
    try {
      await apiSend(`/api/emprunts/${returning.id}/retour`, "POST");
      toast.success(t("toast.loan.returned"));
      setReturning(null);
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
        title={t("page.emprunts.title")}
        subtitle={t("page.emprunts.sub")}
        actions={
          isAdmin ? (
            <Button onClick={() => setCreating(true)}>
              <Plus />
              {t("loan.new")}
            </Button>
          ) : null
        }
      />

      <div className="overflow-hidden rounded-(--radius-lg) border border-line-soft bg-surface shadow-(--shadow-card)">
        {loans.length === 0 ? (
          <EmptyState icon={Clock} title={t("loan.empty.title")} message={t("loan.empty.sub")} />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("loan.reader")}</TableHead>
                <TableHead>{t("loan.book")}</TableHead>
                <TableHead>{t("loan.borrowedOn")}</TableHead>
                <TableHead>{t("loan.dueOn")}</TableHead>
                <TableHead>{t("book.status")}</TableHead>
                {isAdmin && <TableHead className="text-right">{""}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {loans.map((loan) => (
                <TableRow key={loan.id}>
                  <TableCell>
                    <span className="flex items-center gap-3">
                      <Avatar name={loan.lecteur} size={34} />
                      <span className="font-semibold text-ink">{loan.lecteur}</span>
                    </span>
                  </TableCell>
                  <TableCell className="max-w-64 truncate">{loan.livre}</TableCell>
                  <TableCell className="whitespace-nowrap">{d(loan.dateEmprunt)}</TableCell>
                  <TableCell className="whitespace-nowrap">{d(loan.dateRetour)}</TableCell>
                  <TableCell>
                    <LoanStatus loan={loan} />
                  </TableCell>
                  {isAdmin && (
                    <TableCell className="text-right">
                      {loan.dateRetourLivre === null && (
                        <Button variant="secondary" size="sm" onClick={() => setReturning(loan)}>
                          <Undo2 />
                          <span className="hidden sm:inline">{t("loan.return")}</span>
                        </Button>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <LoanDialog open={creating} onOpenChange={setCreating} onDone={() => router.refresh()} />

      <ConfirmDialog
        open={returning !== null}
        onOpenChange={(open) => !open && setReturning(null)}
        title={t("confirm.return.title")}
        description={t("confirm.return.body", returning?.livre ?? "")}
        confirmLabel={t("action.confirm")}
        pending={pending}
        onConfirm={confirmReturn}
      />
    </>
  );
}

/** Turns a due date into a status pill, so the row says what to do at a glance. */
function LoanStatus({ loan }: { loan: Loan }) {
  const { t } = useI18n();

  if (loan.dateRetourLivre !== null) {
    return <Badge variant="neutral">{t("loan.status.returned")}</Badge>;
  }
  if (!loan.dateRetour) {
    return <Badge variant="info">{t("loan.status.active")}</Badge>;
  }

  const days = daysUntil(loan.dateRetour);
  if (days < 0) {
    return (
      <Badge variant="danger" dot>
        {t("loan.overdueBy", -days)}
      </Badge>
    );
  }
  if (days === 0) {
    return (
      <Badge variant="warning" dot>
        {t("loan.dueToday")}
      </Badge>
    );
  }
  if (days <= 3) {
    return (
      <Badge variant="warning" dot>
        {t("loan.dueIn", days)}
      </Badge>
    );
  }
  return (
    <Badge variant="success" dot>
      {t("loan.dueIn", days)}
    </Badge>
  );
}
