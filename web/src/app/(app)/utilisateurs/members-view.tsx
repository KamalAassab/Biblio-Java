"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Trash2, Users } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/confirm-dialog";
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
import { formatPhone } from "@/lib/utils";
import type { Member } from "@/lib/queries";

export function MembersView({
  members,
  currentUserId,
}: {
  members: Member[];
  currentUserId: number;
}) {
  const { t } = useI18n();
  const router = useRouter();

  const [deleting, setDeleting] = React.useState<Member | null>(null);
  const [pending, setPending] = React.useState(false);

  async function confirmDelete() {
    if (!deleting) return;
    setPending(true);
    try {
      await apiSend(`/api/utilisateurs/${deleting.id}`, "DELETE");
      toast.success(t("toast.user.deleted"));
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
      <PageHeader title={t("page.utilisateurs.title")} subtitle={t("page.utilisateurs.sub")} />

      <div className="overflow-hidden rounded-(--radius-lg) border border-line-soft bg-surface shadow-(--shadow-card)">
        {members.length === 0 ? (
          <EmptyState icon={Users} title={t("user.empty.title")} message={t("user.empty.sub")} />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("user.name")}</TableHead>
                <TableHead>{t("user.email")}</TableHead>
                <TableHead>{t("user.phone")}</TableHead>
                <TableHead>{t("user.role")}</TableHead>
                <TableHead className="text-right">{""}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {members.map((member) => (
                <TableRow key={member.id}>
                  <TableCell>
                    <span className="flex items-center gap-3">
                      <Avatar name={member.nom} size={34} />
                      <span className="font-semibold text-ink">{member.nom}</span>
                    </span>
                  </TableCell>
                  <TableCell className="max-w-56 truncate">{member.email || "—"}</TableCell>
                  <TableCell className="whitespace-nowrap">{formatPhone(member.numero)}</TableCell>
                  <TableCell>
                    <Badge variant={member.role === "Admin" ? "info" : "neutral"}>
                      {t(member.role === "Admin" ? "user.role.admin" : "user.role.reader")}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    {/* Deleting your own account would invalidate the session in use. */}
                    {member.id !== currentUserId && (
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => setDeleting(member)}
                        aria-label={t("action.delete")}
                        className="text-ink-muted hover:bg-bad-soft hover:text-bad"
                      >
                        <Trash2 />
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title={t("confirm.delete.user.title")}
        description={t("confirm.delete.user.body", deleting?.nom ?? "")}
        confirmLabel={t("action.delete")}
        destructive
        pending={pending}
        onConfirm={confirmDelete}
      />
    </>
  );
}
