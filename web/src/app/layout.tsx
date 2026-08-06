import type { Metadata, Viewport } from "next";
import { Inter } from "next/font/google";
import { Providers } from "@/components/providers";
import "./globals.css";

// Self-hosted by next/font, so the CSP needs no third-party font origin.
const inter = Inter({
  subsets: ["latin", "latin-ext"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "BiblioTech — Bibliothèque FST Settat",
    template: "%s · BiblioTech",
  },
  description:
    "Gestion de bibliothèque universitaire — catalogue, emprunts et réservations. " +
    "Projet Java réalisé à la Faculté des Sciences et Techniques de Settat par Kamal Aassab.",
  authors: [{ name: "Kamal Aassab", url: "https://kamal-aassab.vercel.app/" }],
  creator: "Kamal Aassab",
  applicationName: "BiblioTech",
  keywords: ["bibliothèque", "library management", "FST Settat", "Java", "Next.js"],
  openGraph: {
    title: "BiblioTech — Bibliothèque FST Settat",
    description:
      "Catalogue, emprunts et réservations réunis dans une interface unique et rapide.",
    type: "website",
    locale: "fr_MA",
  },
  // A demo instance holds no content worth indexing, and search results pointing at
  // seeded data would be misleading.
  robots: { index: false, follow: false },
};

export const viewport: Viewport = {
  themeColor: "#efeae0",
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr" suppressHydrationWarning>
      <body className={`${inter.variable} antialiased`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
