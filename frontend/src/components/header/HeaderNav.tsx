"use client";

import { Link } from "@/i18n/navigation";
import { Menu } from "lucide-react";
import { Fragment } from "react";
import { usePathname } from "next/navigation";
import type { HeaderNavPage } from "@/config/headerNavConfig";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/main-dropdown-menu";
import { useTranslations } from "next-intl";

type HeaderNavProps = {
  pages: HeaderNavPage[];
} & (
  | { variant: "mobile"; className?: string }
  | { variant: "desktop"; className?: string }
);

function resolveSubpageLabel(
  subpage: HeaderNavPage["subpages"][number],
  t: ReturnType<typeof useTranslations<"headerNav">>,
): string {
  if (subpage.name) return subpage.name;
  if (subpage.labelKey) return t(subpage.labelKey);
  return "";
}

function resolvePageLabel(
  page: HeaderNavPage,
  t: ReturnType<typeof useTranslations<"headerNav">>,
): string {
  if (page.title) return page.title;
  if (page.labelKey) return t(page.labelKey);
  return "";
}

/** MainHeader extras 등 하위 항목이 없으면 드롭다운 없이 바로 이동 */
function isLeafPage(page: HeaderNavPage): boolean {
  return (
    page.subpages.length === 0 &&
    !page.allSetsHref &&
    !page.allProductsHref
  );
}

const desktopNavButtonClassName =
  "bg-[#428bca] text-white h-full hover:bg-white hover:text-black font-bold rounded-none";

function SubpageLinks({ page }: { page: HeaderNavPage }) {
  const t = useTranslations("headerNav");
  const hasAllSetsSubpage = page.subpages.some(
    (subpage) => subpage.href === page.allSetsHref,
  );
  const hasAllProductsSubpage = page.subpages.some(
    (subpage) => subpage.href === page.allProductsHref,
  );
  return (
    <>
      {page.subpages.map((subpage, index) => (
        <Fragment key={subpage.id}>
          {subpage.href === page.allSetsHref && index > 0 && (
            <DropdownMenuSeparator />
          )}
          <DropdownMenuItem asChild>
            <Link href={subpage.href}>{resolveSubpageLabel(subpage, t)}</Link>
          </DropdownMenuItem>
        </Fragment>
      ))}
      {page.allSetsHref && !hasAllSetsSubpage && (
        <>
          <DropdownMenuSeparator />
          <DropdownMenuItem asChild>
            <Link href={page.allSetsHref}>{t("allSets")}</Link>
          </DropdownMenuItem>
        </>
      )}
      {page.allProductsHref && (
        <>
          <DropdownMenuSeparator />
          <DropdownMenuItem asChild>
            <Link href={page.allProductsHref}>{t("allProducts")}</Link>
          </DropdownMenuItem>
        </>
      )}
    </>
  );
}

function MobileNav({
  pages,
  className,
}: {
  pages: HeaderNavPage[];
  className?: string;
}) {
  const t = useTranslations("headerNav");

  return (
    <div className={cn("flex flex-row justify-center", className)}>
      <DropdownMenu modal={false}>
        <DropdownMenuTrigger asChild>
          <Button className="bg-black text-white">
            <Menu />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuGroup>
            {pages.map((page) =>
              isLeafPage(page) ? (
                <DropdownMenuItem key={page.id} asChild>
                  <Link href={page.href}>{resolvePageLabel(page, t)}</Link>
                </DropdownMenuItem>
              ) : (
                <DropdownMenuSub key={page.id}>
                  <DropdownMenuSubTrigger>
                    {resolvePageLabel(page, t)}
                  </DropdownMenuSubTrigger>
                  <DropdownMenuSubContent>
                    <SubpageLinks page={page} />
                  </DropdownMenuSubContent>
                </DropdownMenuSub>
              ),
            )}
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

function DesktopNav({
  pages,
  className,
}: {
  pages: HeaderNavPage[];
  className?: string;
}) {
  const t = useTranslations("headerNav");

  return (
    <div className={className}>
      <div className="flex flex-row flex-nowrap items-center justify-center bg-[#428bca] h-16">
        {pages.map((page) =>
          isLeafPage(page) ? (
            <Button
              key={page.id}
              asChild
              className={desktopNavButtonClassName}
            >
              <Link href={page.href}>{resolvePageLabel(page, t)}</Link>
            </Button>
          ) : (
            <DropdownMenu key={page.id} modal={false}>
              <DropdownMenuTrigger asChild>
                <Button className={desktopNavButtonClassName}>
                  {resolvePageLabel(page, t)}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent>
                <DropdownMenuGroup>
                  <SubpageLinks page={page} />
                </DropdownMenuGroup>
              </DropdownMenuContent>
            </DropdownMenu>
          ),
        )}
      </div>
    </div>
  );
}

export default function HeaderNav(props: HeaderNavProps) {
  const pathname = usePathname();
  if (props.variant === "mobile") {
    return (
      <MobileNav
        key={pathname}
        pages={props.pages}
        className={props.className}
      />
    );
  }
  return (
    <DesktopNav
      key={pathname}
      pages={props.pages}
      className={props.className}
    />
  );
}
