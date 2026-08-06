import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,

  // Never leak the framework version in response headers.
  poweredByHeader: false,

  // Trailing-slash redirects are a small but free source of open-redirect surface.
  trailingSlash: false,

  experimental: {
    // Import only the icons actually used, rather than the whole lucide barrel.
    optimizePackageImports: ["lucide-react", "framer-motion"],
  },

  // Type errors must fail the build — a deploy should never ship code that does
  // not compile. (Next 16 no longer accepts an `eslint` key here; linting runs
  // via `npm run lint`.)
  typescript: {
    ignoreBuildErrors: false,
  },
};

export default nextConfig;
