"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { BookMarked, BookOpen, CheckCircle2, Clock, Plus, Search } from "lucide-react";
import { BookCard } from "@/components/book-card";
import { BookDialog } from "@/components/book-dialog";
import { LoanDialog, ReservationDialog } from "@/components/record-dialogs";
import {
  ActionCard,
  EmptyState,
  PageHeader,
  SectionHeading,
  StatCard,
} from "@/components/page-furniture";
import { Button } from "@/components/ui/button";
import { useI18n } from "@/components/providers";
import type { Book, Stats } from "@/lib/queries";

export function DashboardView({
  name,
  isAdmin,
  stats,
  recent,
}: {
  name: string;
  isAdmin: boolean;
  stats: Stats;
  recent: Book[];
}) {
  const { t } = useI18n();
  const router = useRouter();

  const [viewing, setViewing] = React.useState<Book | null>(null);
  const [creatingBook, setCreatingBook] = React.useState(false);
  const [creatingLoan, setCreatingLoan] = React.useState(false);
  const [creatingReservation, setCreatingReservation] = React.useState(false);

  const refresh = React.useCallback(() => router.refresh(), [router]);

  const greeting = React.useMemo(() => {
    const hour = new Date().getHours();
    const key =
      hour < 12 ? "dash.greeting.morning" : hour < 18 ? "dash.greeting.afternoon" : "dash.greeting.evening";
    return t(key, name);
  }, [name, t]);

  return (
    <>
      <PageHeader title={greeting} subtitle={t("page.dashboard.sub")} />

      <section className="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <StatCard label={t("dash.stat.books")} value={stats.books} icon={BookOpen} tone="navy" index={0} />
        <StatCard
          label={t("dash.stat.available")}
          value={stats.available}
          caption={t("dash.availability", stats.available, stats.books)}
          icon={CheckCircle2}
          tone="ok"
          index={1}
        />
        <StatCard
          label={t("dash.stat.loans")}
          value={stats.activeLoans}
          caption={stats.overdue > 0 ? `${t("dash.stat.overdue")} : ${stats.overdue}` : null}
          icon={Clock}
          tone="warn"
          index={2}
        />
        <StatCard
          label={t("dash.stat.reservations")}
          value={stats.reservations}
          icon={BookMarked}
          tone="gold"
          index={3}
        />
      </section>

      <section className="pt-8">
        <SectionHeading
          title={t("dash.recent.title")}
          action={
            <Button variant="secondary" size="sm" onClick={() => router.push("/catalogue")}>
              {t("dash.viewAll")}
            </Button>
          }
        />

        {recent.length === 0 ? (
          <div className="rounded-(--radius-lg) border border-line-soft bg-surface shadow-(--shadow-card)">
            <EmptyState
              icon={BookOpen}
              title={t("cat.empty.none")}
              message={t("cat.empty.none.sub")}
              action={
                isAdmin ? (
                  <Button onClick={() => setCreatingBook(true)}>
                    <Plus />
                    {t("cat.add")}
                  </Button>
                ) : null
              }
            />
          </div>
        ) : (
          // A horizontal shelf on wide screens; wraps into a grid on small ones so
          // nothing is hidden behind a scroll gesture on a phone.
          <div className="-mx-1 flex snap-x snap-mandatory gap-4 overflow-x-auto scroll-none px-1 pb-2">
            {recent.map((book, index) => (
              <div key={book.id} className="w-[10.5rem] shrink-0 snap-start sm:w-[12.5rem]">
                <BookCard book={book} onOpen={setViewing} index={index} />
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="pt-8">
        <SectionHeading title={t("dash.actions.title")} />
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <ActionCard
            icon={Search}
            title={t("dash.action.browse")}
            subtitle={t("dash.action.browse.sub")}
            tone="navy"
            onClick={() => router.push("/catalogue")}
            index={0}
          />
          {isAdmin && (
            <ActionCard
              icon={Plus}
              title={t("dash.action.addBook")}
              subtitle={t("dash.action.addBook.sub")}
              tone="ok"
              onClick={() => setCreatingBook(true)}
              index={1}
            />
          )}
          {isAdmin && (
            <ActionCard
              icon={Clock}
              title={t("dash.action.newLoan")}
              subtitle={t("dash.action.newLoan.sub")}
              tone="warn"
              onClick={() => setCreatingLoan(true)}
              index={2}
            />
          )}
          {isAdmin && (
            <ActionCard
              icon={BookMarked}
              title={t("dash.action.newReservation")}
              subtitle={t("dash.action.newReservation.sub")}
              tone="gold"
              onClick={() => setCreatingReservation(true)}
              index={3}
            />
          )}
        </div>
      </section>

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
        open={creatingBook}
        isAdmin={isAdmin}
        onOpenChange={setCreatingBook}
        onDone={refresh}
      />
      <LoanDialog open={creatingLoan} onOpenChange={setCreatingLoan} onDone={refresh} />
      <ReservationDialog
        open={creatingReservation}
        onOpenChange={setCreatingReservation}
        onDone={refresh}
      />
    </>
  );
}
