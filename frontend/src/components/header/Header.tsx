"use client";

import NextLink from "next/link";
import { useRouter, Link } from "@/i18n/navigation";
import { usePathname, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ShoppingCart, User } from "lucide-react";
import { useCartStore } from "@/stores/cart-store";
import { useLocale, useTranslations } from "next-intl";
import AuthButton from "./AuthButton";
import { useAuthStore } from "@/stores/auth-store";
import HeaderSearchBar from "../header/HeaderSearchBar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/main-dropdown-menu";
import HeaderNav from "../header/HeaderNav";
import { headerNavPages } from "@/config/headerNavConfig";
import { routing } from "@/i18n/routing";
import { api } from "@/lib/api.client";
import type { HeaderNavPage } from "@/config/headerNavConfig";
import Image from "next/image";
import { resolveAssetUrl } from "@/lib/public-asset-url";
import { shouldRedirectHomeAfterLogout } from "@/lib/logout-redirect";

type HeaderNavSetApiDto = {
  setCode: string;
  setName: string;
  urlName?: string | null;
};

type HeaderNavGameSetsApiDto = {
  gameCode: "mtg" | "fab" | "swu" | "lorc" | "rift";

  sets: HeaderNavSetApiDto[];
};

type HeaderNavExtraApiDto = {
  id: number;
  title: string;
  urlString: string;
  displayOrder: number;
};

const HEADER_GAME_PAGE_ID_BY_CODE: Record<
  HeaderNavGameSetsApiDto["gameCode"],
  number
> = {
  mtg: 1,
  fab: 2,
  swu: 3,
  lorc: 4,
  rift: 5,
};

/** DB extras id와 게임 하드코딩 id 충돌 방지용 offset */
const EXTRA_PAGE_ID_OFFSET = 1000;

function buildSetPageHref(
  gameCode: HeaderNavGameSetsApiDto["gameCode"],
  set: HeaderNavSetApiDto,
): string {
  const segment =
    gameCode === "mtg" ? set.setCode : (set.urlName ?? set.setCode);
  return `/game/${gameCode}/${encodeURIComponent(segment)}`;
}

function buildHeaderNavPages(
  basePages: HeaderNavPage[],
  latestSetsByGame: HeaderNavGameSetsApiDto[],
): HeaderNavPage[] {
  if (!latestSetsByGame.length) return basePages;

  return basePages.map((page) => {
    if (page.skipLatestSets) return page;

    const matched = latestSetsByGame.find(
      (item) => HEADER_GAME_PAGE_ID_BY_CODE[item.gameCode] === page.id,
    );
    if (!matched || matched.sets.length === 0) return page;

    const subpages = matched.sets.map((set, index) => ({
      id: index + 1,
      name: set.setName,
      href: buildSetPageHref(matched.gameCode, set),
    }));

    const staticSubpages = page.subpages.map((subpage, index) => ({
      ...subpage,
      id: subpages.length + index + 1,
    }));

    return { ...page, subpages: [...subpages, ...staticSubpages] };
  });
}

function extrasToHeaderNavPages(extras: HeaderNavExtraApiDto[]): HeaderNavPage[] {
  return extras.map((extra) => ({
    id: EXTRA_PAGE_ID_OFFSET + extra.id,
    title: extra.title,
    href: extra.urlString,
    /** 하위 메뉴 없음 → HeaderNav에서 Link 버튼으로 렌더 */
    subpages: [],
  }));
}

/** next/navigation pathname(로케일 포함)에서 locale segment 제거 → next-intl router.push/replace용. */
function pathnameWithoutLocale(pathWithLocale: string): string {
  for (const loc of routing.locales) {
    if (pathWithLocale === `/${loc}`) return "/";
    if (pathWithLocale.startsWith(`/${loc}/`)) {
      const rest = pathWithLocale.slice(`/${loc}`.length);
      return rest || "/";
    }
  }
  return pathWithLocale || "/";
}

export default function Header() {
  const itemCount = useCartStore((s) => s.itemCount);
  const t = useTranslations();
  const locale = useLocale();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const otherLocale = locale === "ko" ? "en" : "ko";
  const router = useRouter();
  const pathname = usePathname() ?? "";
  const searchParams = useSearchParams();
  const querySuffix = useMemo(() => {
    if (!searchParams) return "";
    const q = searchParams.toString();
    return q ? `?${q}` : "";
  }, [searchParams]);
  const [pages, setPages] = useState(headerNavPages);

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      const [setsResult, extrasResult] = await Promise.allSettled([
        api.get<HeaderNavGameSetsApiDto[]>("/api/main/header-nav/sets?limit=6"),
        api.get<HeaderNavExtraApiDto[]>("/api/main/header-nav/extras"),
      ]);

      if (!mounted) return;

      const sets =
        setsResult.status === "fulfilled" ? setsResult.value : [];
      const extras =
        extrasResult.status === "fulfilled" ? extrasResult.value : [];

      const gamePages = buildHeaderNavPages(headerNavPages, sets);
      const extraPages = extrasToHeaderNavPages(extras);
      setPages([...gamePages, ...extraPages]);
    };

    void load();

    return () => {
      mounted = false;
    };
  }, []);

  const toggleLanguage = useCallback(() => {
    const bare = pathnameWithoutLocale(pathname);
    // replace는 현재 히스토리 항목을 덮어서, 뒤로 가기로 직전 로케일로 돌아갈 수 없음 → push 유지
    router.push(`${bare}${querySuffix}`, { locale: otherLocale });
  }, [pathname, querySuffix, otherLocale, router]);

  const handleLogout = useCallback(() => {
    const redirectHome = shouldRedirectHomeAfterLogout(pathname);
    logout({
      onComplete: () => {
        if (redirectHome) {
          router.push("/");
        }
      },
    });
  }, [logout, pathname, router]);

  const cartLink = (
    <Link href="/cart" className="flex shrink-0 items-center gap-1">
      <ShoppingCart className="h-7 w-7" />
      {itemCount > 0 && (
        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary text-xs text-primary-foreground">
          {itemCount}
        </span>
      )}
    </Link>
  );

  const languageButton = (
    <Button className="shrink-0 bg-black" size="sm" onClick={toggleLanguage}>
      {otherLocale.toUpperCase()}
    </Button>
  );

  return (
    <header className="sticky top-0 z-50 bg-white">
      <div className="flex flex-col gap-2 p-4 sm:px-6 lg:gap-4 lg:px-8">
        <div className="flex min-w-0 items-center gap-2 sm:gap-4">
          <HeaderNav
            variant="mobile"
            pages={pages}
            className="shrink-0 lg:hidden"
          />
          <Link
            href="/"
            className="flex min-w-0 flex-1 justify-center font-semibold lg:flex-none lg:justify-start"
          >
            <Image
              src={resolveAssetUrl("/uploads/assets/mainlogo.webp")}
              alt={t("common.logoAlt")}
              width={280}
              height={80}
              sizes="(max-width: 640px) 140px, (max-width: 1024px) 220px, 280px"
              className="h-auto w-[140px] sm:w-[220px] lg:w-[280px]"
            />
          </Link>
          <div className="hidden min-w-0 flex-1 md:flex">
            <HeaderSearchBar />
          </div>
          <nav className="flex shrink-0 items-center gap-1 sm:gap-2 lg:gap-4">
            <div className="flex items-center gap-1 sm:gap-2 lg:hidden">
              {cartLink}
              {languageButton}
              {isAuthenticated ? (
                <DropdownMenu modal={false}>
                  <DropdownMenuTrigger asChild>
                    <Button
                      size="icon"
                      variant="outline"
                      aria-label={t("nav.mypage")}
                    >
                      <User className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {user?.role === "ROLE_ADMIN" && (
                      <DropdownMenuItem asChild>
                        <NextLink href="/admin">{t("nav.admin")}</NextLink>
                      </DropdownMenuItem>
                    )}
                    <DropdownMenuItem asChild>
                      <Link href="/mypage/main">{t("nav.mypage")}</Link>
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={handleLogout}>
                      {t("nav.logout")}
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              ) : (
                <AuthButton />
              )}
            </div>
            <div className="hidden items-center gap-2 text-sm lg:flex lg:text-base">
              {isAuthenticated && user?.role === "ROLE_ADMIN" ? (
                <NextLink href="/admin">{t("nav.admin")}</NextLink>
              ) : null}
              {isAuthenticated ? (
                <Link href="/mypage/main">
                  <Button size="sm" variant="outline">
                    {t("nav.mypage")}
                  </Button>
                </Link>
              ) : null}
              {!isAuthenticated ? (
                <Link href="/guest-order-check">
                  <Button size="sm" variant="outline">
                    {t("nav.guestordercheck")}
                  </Button>
                </Link>
              ) : null}
              {cartLink}
              <AuthButton />
              {languageButton}
            </div>
          </nav>
        </div>
        <div className="w-full md:hidden">
          <HeaderSearchBar />
        </div>
      </div>
      <HeaderNav variant="desktop" pages={pages} className="hidden lg:block" />
    </header>
  );
}
