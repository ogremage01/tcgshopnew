import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  images: {
    remotePatterns: [],
  },
  async rewrites() {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL
    if (apiUrl) return []
    return [
      // SSE Route Handler는 rewrite가 가로채면 500/버퍼링
      {
        source: "/api/:path((?!admin/alarm/subscribe).*)",
        destination: "http://127.0.0.1:18567/api/:path",
      },
      {
        source: "/card-images/:path*",
        destination: "http://127.0.0.1:18567/card-images/:path*",
      },
      {
        source: "/uploads/:path*",
        destination: "http://127.0.0.1:18567/uploads/:path*",
      },
    ]
  },
}

export default withNextIntl(nextConfig)
