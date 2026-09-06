"use client";

// locale에 따라 단일 버튼으로 한글/영문 API를 분기하던 로직. 복원 시 아래를 해제한다.
// import { useLocale, useTranslations } from "next-intl";
import { useCallback, useRef, useState } from "react";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { ScrollArea } from "../ui/scroll-area";
import PaginationRangeAsync from "../ui/pagination-range-async";

const COUNT_PER_PAGE = 10;

export type PostalCodeApiLocale = "ko" | "en";

export type PostalCodeSelection = {
  postalCode: string;
  address: string;
};

type JusoItem = {
  zipNo?: string;
  roadAddr?: string;
  jibunAddr?: string;
};

type JusoSearchResponse = {
  results?: {
    common?: {
      currentPage?: string;
      countPerPage?: string;
      errorCode?: string;
      errorMessage?: string;
      totalCount?: string;
    };
    juso?: JusoItem[];
  };
};

const DIALOG_COPY = {
  ko: {
    trigger: "주소 입력",
    title: "주소 입력",
    placeholder: "도로명, 건물명, 지번으로 검색",
    search: "검색",
    noResults: "검색 결과가 없습니다.",
  },
  en: {
    trigger: "enter address",
    title: "enter address",
    placeholder: "Search by road name, building, or lot number",
    search: "Search",
    noResults: "No results found.",
  },
} as const;

type PostalCodeSearchDialogProps = {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  onSelect: (selection: PostalCodeSelection) => void;
  apiLocale: PostalCodeApiLocale;
};

function calcTotalPages(totalCount: string | undefined, countPerPage: string | undefined) {
  const total = Number(totalCount ?? 0);
  const perPage = Number(countPerPage ?? COUNT_PER_PAGE);
  if (total <= 0 || perPage <= 0) return 1;
  return Math.ceil(total / perPage);
}

export default function PostalCodeSearchDialog({
  open: openProp,
  onOpenChange,
  onSelect,
  apiLocale,
}: PostalCodeSearchDialogProps) {
  // 로케일에 따라 영문/한글 주소 API를 호출하던 로직. 복원 시 아래 주석을 해제하고 apiLocale prop을 제거한다.
  // const locale = useLocale();
  // 영어 로케일에서도 한글 주소 API만 사용하던 로직. 복원 시 아래 주석을 해제한다.
  // const locale = "ko";
  const locale = apiLocale;
  const copy = DIALOG_COPY[apiLocale];
  // const t = useTranslations("checkoutPage");
  const scrollAreaRef = useRef<HTMLDivElement>(null);
  const [uncontrolledOpen, setUncontrolledOpen] = useState(false);
  const isControlled = openProp !== undefined;
  const open = isControlled ? openProp : uncontrolledOpen;
  const [results, setResults] = useState<JusoItem[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [inputKeyword, setInputKeyword] = useState("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [hasSearched, setHasSearched] = useState(false);

  const scrollToTop = useCallback(() => {
    const viewport = scrollAreaRef.current?.querySelector(
      "[data-radix-scroll-area-viewport]",
    );
    if (viewport instanceof HTMLElement) {
      viewport.scrollTop = 0;
    }
  }, []);

  const resetDialogState = useCallback(() => {
    setInputKeyword("");
    setSearchKeyword("");
    setResults([]);
    setIsSearching(false);
    setCurrentPage(1);
    setTotalPages(1);
    setHasSearched(false);
    scrollToTop();
  }, [scrollToTop]);

  const handleOpenChange = useCallback(
    (nextOpen: boolean) => {
      resetDialogState();
      if (!isControlled) {
        setUncontrolledOpen(nextOpen);
      }
      onOpenChange?.(nextOpen);
    },
    [isControlled, onOpenChange, resetDialogState],
  );

  const fetchResults = async (searchKeyword: string, page: number) => {
    setIsSearching(true);
    try {
      const params = new URLSearchParams({
        keyword: searchKeyword,
        locale,
        currentPage: String(page),
        countPerPage: String(COUNT_PER_PAGE),
      });
      const response = await fetch(`/postal-code?${params.toString()}`);

      if (process.env.NODE_ENV === "development") {
        console.log("[postal-code] confmKey:", response.headers.get("X-Debug-ConfmKey"));
        console.log("[postal-code] targetUrl:", response.headers.get("X-Debug-Target-Url"));
      }

      if (!response.ok) {
        const errorBody = await response.json().catch(() => null);
        console.error("[postal-code] request failed:", response.status, errorBody);
        setResults([]);
        setTotalPages(1);
        setCurrentPage(1);
        return;
      }

      const data = (await response.json()) as JusoSearchResponse;
      console.log("[postal-code] response:", data);

      const common = data.results?.common;
      if (common?.errorCode && common.errorCode !== "0") {
        console.warn("[postal-code] API error:", common.errorMessage);
        setResults([]);
        setTotalPages(1);
        setCurrentPage(1);
        return;
      }

      const nextTotalPages = calcTotalPages(common?.totalCount, common?.countPerPage);
      const nextCurrentPage = Number(common?.currentPage ?? page);

      setResults(data.results?.juso ?? []);
      setTotalPages(nextTotalPages);
      setCurrentPage(nextCurrentPage);
      setHasSearched(true);
      requestAnimationFrame(() => scrollToTop());
    } catch (error) {
      console.error("[postal-code] parse error:", error);
      setResults([]);
      setTotalPages(1);
      setCurrentPage(1);
    } finally {
      setIsSearching(false);
    }
  };

  const handleSearch = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const nextKeyword = inputKeyword.trim();
    if (!nextKeyword) return;

    setSearchKeyword(nextKeyword);
    await fetchResults(nextKeyword, 1);
  };

  const handlePageChange = (page: number) => {
    if (!searchKeyword || page === currentPage || page < 1 || page > totalPages) return;
    void fetchResults(searchKeyword, page);
  };

  const handleSelect = (item: JusoItem) => {
    const postalCode = item.zipNo?.trim() ?? "";
    const address = item.roadAddr?.trim() ?? "";
    if (!postalCode || !address) return;

    onSelect({ postalCode, address });
    handleOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        {/* locale에 따른 단일 버튼. 복원 시 아래를 해제하고 copy.trigger를 되돌린다. */}
        {/* <Button type="button">{t("search_postal_code")}</Button> */}
        <Button type="button">{copy.trigger}</Button>
      </DialogTrigger>
      <DialogContent className="flex h-[min(80vh,720px)] max-w-3xl flex-col gap-0 overflow-hidden p-0 sm:max-w-3xl">
        <DialogHeader className="border-b px-6 py-4">
          {/* <DialogTitle>{t("search_postal_code")}</DialogTitle> */}
          <DialogTitle>{copy.title}</DialogTitle>
        </DialogHeader>
        <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
          <form onSubmit={handleSearch} className="m-2 flex shrink-0 flex-row gap-2">
            <Input
              placeholder={copy.placeholder}
              type="text"
              inputMode="text"
              name="keyword"
              value={inputKeyword}
              onChange={(e) => setInputKeyword(e.target.value)}
            />
            <Button type="submit" disabled={isSearching}>
              {copy.search}
            </Button>
          </form>
          <ScrollArea ref={scrollAreaRef} className="h-0 min-h-0 flex-1">
            <div className="flex flex-col gap-2 p-2 pr-4">
              {!hasSearched ? null : results.length === 0 ? (
                <span>{copy.noResults}</span>
              ) : (
                results.map((item, index) => (
                  <div
                    key={`${item.zipNo}-${index}`}
                    className="rounded-md border p-3 hover:bg-muted"
                  >
                    <button
                      type="button"
                      className="w-full touch-pan-y text-left"
                      onClick={() => handleSelect(item)}
                    >
                      <div className="flex flex-col gap-1">
                        <div className="text-sm font-medium">{item.roadAddr}</div>
                        <div className="text-xs text-muted-foreground">{item.jibunAddr}</div>
                        <div className="text-xs text-muted-foreground">{item.zipNo}</div>
                      </div>
                    </button>
                  </div>
                ))
              )}
            </div>
          </ScrollArea>
          {totalPages > 1 && (
            <div className="shrink-0 border-t px-4 py-3">
              <PaginationRangeAsync
                currentPage={currentPage}
                totalPages={totalPages}
                siblingCount={2}
                onPageChange={handlePageChange}
              />
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
