import createMiddleware from "next-intl/middleware";
import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import { routing } from "./i18n/routing";

const intlMiddleware = createMiddleware(routing);

const duplicateLocalePrefix = /^\/(ko|en)\/(ko|en)(\/.*)?$/;

export default function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  if (pathname.startsWith("/admin")) {
    return NextResponse.next();
  }

  const dup = pathname.match(duplicateLocalePrefix);
  if (dup) {
    const url = request.nextUrl.clone();
    url.pathname = `/${dup[1]}${dup[3] ?? ""}`;
    return NextResponse.redirect(url);
  }

  return intlMiddleware(request);
}

export const config = {
  matcher: ["/((?!api|_next|_vercel|admin|postal-code|.*\\..*).*)"],
};
