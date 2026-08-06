import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { LoginView } from "./login-view";

export const metadata: Metadata = { title: "Connexion" };

export default async function LoginPage() {
  // Belt and braces: middleware also redirects, but it only checks that a cookie
  // exists. This verifies the signature before deciding.
  if (await getSession()) redirect("/dashboard");
  return <LoginView demoMode={process.env.DEMO_MODE === "true"} />;
}
