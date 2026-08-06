"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { BookMarked, Clock, Mail, Phone, User } from "lucide-react";
import { toast } from "sonner";
import { PageHeader, StatCard } from "@/components/page-furniture";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input, PasswordInput } from "@/components/ui/input";
import { Avatar, Label } from "@/components/ui/misc";
import { useI18n, useSession } from "@/components/providers";
import { ApiError, apiSend } from "@/lib/client";

interface ProfileUser {
  id: number;
  nom: string;
  email: string;
  role: "Admin" | "Lecteur";
  numero: number;
}

export function ProfileView({
  user,
  activity,
}: {
  user: ProfileUser;
  activity: { loans: number; reservations: number };
}) {
  const { t } = useI18n();
  const router = useRouter();
  const { setUser } = useSession();

  const [nom, setNom] = React.useState(user.nom);
  const [email, setEmail] = React.useState(user.email);
  const [numero, setNumero] = React.useState(user.numero ? String(user.numero) : "");
  const [savingDetails, setSavingDetails] = React.useState(false);
  const [detailsError, setDetailsError] = React.useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = React.useState("");
  const [newPassword, setNewPassword] = React.useState("");
  const [confirmPassword, setConfirmPassword] = React.useState("");
  const [savingPassword, setSavingPassword] = React.useState(false);
  const [passwordError, setPasswordError] = React.useState<string | null>(null);

  async function saveDetails(event: React.FormEvent) {
    event.preventDefault();
    setSavingDetails(true);
    setDetailsError(null);
    try {
      const { user: updated } = await apiSend<{ user: ProfileUser }>("/api/profile", "PUT", {
        nom,
        email,
        numero: numero ? Number(numero.replace(/\D/g, "")) : 0,
      });
      toast.success(t("profile.updated"));
      setUser({ id: updated.id, nom: updated.nom, email: updated.email, role: user.role });
      router.refresh();
    } catch (caught) {
      const error = caught as ApiError;
      setDetailsError(t(error.key ?? "toast.error"));
      toast.error(t(error.key ?? "toast.error"));
    } finally {
      setSavingDetails(false);
    }
  }

  async function changePassword(event: React.FormEvent) {
    event.preventDefault();
    setPasswordError(null);

    if (newPassword !== confirmPassword) {
      setPasswordError(t("profile.password.mismatch"));
      return;
    }

    setSavingPassword(true);
    try {
      await apiSend("/api/profile/password", "PUT", { currentPassword, newPassword });
      toast.success(t("profile.password.changed"));
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (caught) {
      const error = caught as ApiError;
      setPasswordError(t(error.key ?? "toast.error"));
    } finally {
      setSavingPassword(false);
    }
  }

  return (
    <>
      <PageHeader title={t("profile.title")} subtitle={t("profile.sub")} />

      {/* Identity banner */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
        className="overflow-hidden rounded-[--radius-xl] border border-line-soft bg-surface shadow-[--shadow-card]"
      >
        <div className="relative h-28 bg-gradient-to-br from-navy-700 to-navy-600">
          <span className="absolute -right-10 -top-14 size-44 rounded-full bg-gold-500/25" />
        </div>
        <div className="flex flex-col gap-4 px-6 pb-6 sm:flex-row sm:items-end">
          <div className="-mt-12 shrink-0">
            <Avatar name={user.nom} size={88} ring />
          </div>
          <div className="min-w-0 flex-1 sm:pb-1">
            <h2 className="truncate text-2xl font-extrabold tracking-tight text-ink">{user.nom}</h2>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <Badge variant={user.role === "Admin" ? "info" : "neutral"}>
                {t(user.role === "Admin" ? "user.role.admin" : "user.role.reader")}
              </Badge>
              {user.email && <span className="text-sm text-ink-muted">{user.email}</span>}
            </div>
          </div>
        </div>
      </motion.div>

      <section className="grid gap-3 pt-4 sm:grid-cols-2">
        <StatCard label={t("profile.loans")} value={activity.loans} icon={Clock} tone="warn" />
        <StatCard
          label={t("profile.reservations")}
          value={activity.reservations}
          icon={BookMarked}
          tone="gold"
          index={1}
        />
      </section>

      <section className="grid gap-4 pt-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>{t("profile.details")}</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={saveDetails} className="flex flex-col gap-4" noValidate>
              <div className="flex flex-col gap-2">
                <Label htmlFor="nom">{t("user.name")}</Label>
                <Input
                  id="nom"
                  value={nom}
                  onChange={(event) => setNom(event.target.value)}
                  icon={<User />}
                  maxLength={80}
                  autoComplete="name"
                />
              </div>

              <div className="flex flex-col gap-2">
                <Label htmlFor="email">{t("user.email")}</Label>
                <Input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  icon={<Mail />}
                  maxLength={254}
                  autoComplete="email"
                />
              </div>

              <div className="flex flex-col gap-2">
                <Label htmlFor="numero">{t("user.phone")}</Label>
                <Input
                  id="numero"
                  inputMode="numeric"
                  value={numero}
                  onChange={(event) => setNumero(event.target.value)}
                  icon={<Phone />}
                  maxLength={15}
                  autoComplete="tel"
                />
              </div>

              <p className="min-h-4 text-xs font-medium text-bad" role="alert">
                {detailsError ?? ""}
              </p>

              <Button type="submit" loading={savingDetails} className="self-start">
                {t("action.save")}
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t("profile.security")}</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={changePassword} className="flex flex-col gap-4" noValidate>
              <div className="flex flex-col gap-2">
                <Label htmlFor="currentPassword">{t("profile.password.current")}</Label>
                <PasswordInput
                  id="currentPassword"
                  value={currentPassword}
                  onChange={(event) => setCurrentPassword(event.target.value)}
                  autoComplete="current-password"
                />
              </div>

              <div className="flex flex-col gap-2">
                <Label htmlFor="newPassword">{t("profile.password.new")}</Label>
                <PasswordInput
                  id="newPassword"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  autoComplete="new-password"
                />
              </div>

              <div className="flex flex-col gap-2">
                <Label htmlFor="confirmPassword">{t("profile.password.confirm")}</Label>
                <PasswordInput
                  id="confirmPassword"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  autoComplete="new-password"
                  invalid={confirmPassword !== "" && confirmPassword !== newPassword}
                />
              </div>

              <p className="text-[11px] leading-relaxed text-ink-muted">
                {t("profile.hint.password")}
              </p>

              <p className="min-h-4 text-xs font-medium text-bad" role="alert">
                {passwordError ?? ""}
              </p>

              <Button
                type="submit"
                variant="secondary"
                loading={savingPassword}
                className="self-start"
              >
                {t("profile.password.change")}
              </Button>
            </form>
          </CardContent>
        </Card>
      </section>
    </>
  );
}
