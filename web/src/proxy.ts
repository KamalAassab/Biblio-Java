import { NextResponse, type NextRequest } from "next/server";
// Imported from the constants-only module: `@/lib/session` pulls in `node:crypto`,
// which the Edge runtime cannot load.
import { SESSION_COOKIE } from "@/lib/cookie-names";

/**
 * Edge proxy (formerly "middleware"): route guard plus per-response security headers.
 *
 * Important limitation, stated plainly: this only checks that a session cookie is
 * *present*. It runs on the Edge runtime, which has no `node:crypto`, so it cannot
 * verify the HMAC. Real authorisation happens in every route handler and page via
 * `requireSession()` / `requireAdmin()`. This layer exists to redirect signed-out
 * visitors quickly, not to make security decisions.
 */

const PUBLIC_PATHS = ["/login", "/api/auth/login", "/api/auth/session", "/api/auth/logout"];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const isPublic =
    PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`)) ||
    pathname.startsWith("/_next") ||
    pathname === "/favicon.ico";

  const hasSessionCookie = Boolean(request.cookies.get(SESSION_COOKIE)?.value);

  let response: NextResponse;

  if (!isPublic && !hasSessionCookie) {
    if (pathname.startsWith("/api/")) {
      response = NextResponse.json(
        { error: "error.unauthorized" },
        { status: 401, headers: { "cache-control": "no-store" } },
      );
    } else {
      const url = request.nextUrl.clone();
      url.pathname = "/login";
      url.search = pathname === "/" ? "" : `?next=${encodeURIComponent(pathname)}`;
      response = NextResponse.redirect(url);
    }
  } else if (pathname === "/login" && hasSessionCookie) {
    const url = request.nextUrl.clone();
    url.pathname = "/dashboard";
    url.search = "";
    response = NextResponse.redirect(url);
  } else {
    response = NextResponse.next();
  }

  return applySecurityHeaders(response);
}

function applySecurityHeaders(response: NextResponse): NextResponse {
  const headers = response.headers;

  // 'unsafe-inline' on styles is required by Tailwind's runtime style injection and
  // by framer-motion, which writes inline transforms. Scripts stay restricted;
  // 'unsafe-eval' is only tolerated in development, where React refresh needs it.
  const scriptSrc =
    process.env.NODE_ENV === "development"
      ? "'self' 'unsafe-inline' 'unsafe-eval'"
      : "'self' 'unsafe-inline'";

  headers.set(
    "Content-Security-Policy",
    [
      "default-src 'self'",
      `script-src ${scriptSrc}`,
      "style-src 'self' 'unsafe-inline'",
      // Cover artwork is hotlinked from the two catalogue sources that `db:covers`
      // resolves against, named explicitly rather than allowed by wildcard. Images
      // are the only third-party content the app loads.
      "img-src 'self' data: blob: https://covers.openlibrary.org https://books.google.com https://books.googleusercontent.com",
      "font-src 'self' data:",
      // The app talks only to its own origin; no third-party analytics or CDNs.
      "connect-src 'self'",
      "frame-ancestors 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "object-src 'none'",
      "upgrade-insecure-requests",
    ].join("; "),
  );

  headers.set("X-Content-Type-Options", "nosniff");
  headers.set("X-Frame-Options", "DENY");
  headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  headers.set("X-DNS-Prefetch-Control", "off");
  headers.set(
    "Permissions-Policy",
    "camera=(), microphone=(), geolocation=(), payment=(), usb=(), interest-cohort=()",
  );
  headers.set("Cross-Origin-Opener-Policy", "same-origin");
  headers.set("Cross-Origin-Resource-Policy", "same-origin");

  if (process.env.NODE_ENV === "production") {
    headers.set("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
  }

  return response;
}

export const config = {
  matcher: [
    // Everything except Next's static output and image optimiser.
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)",
  ],
};
