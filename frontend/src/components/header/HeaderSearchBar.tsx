"use client";

import {
  useState,
  useCallback,
  useRef,
  useEffect,
  FormEvent,
  KeyboardEvent,
} from "react";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useHeaderSearchSuggest } from "./_hooks/useHeaderSearchSuggest";

/** 헤더(홈 포함)에서 공통으로 사용하는 검색바 — 자동완성 후 /search 로 이동 */
export default function HeaderSearchBar() {
  const router = useRouter();
  const t = useTranslations("search");
  const [value, setValue] = useState("");
  const [activeIndex, setActiveIndex] = useState(-1);
  const blurTimerRef = useRef<number | null>(null);
  const { items, loading, isOpen, setIsOpen, minQueryLength } =
    useHeaderSearchSuggest(value);

  const navigateToSearch = useCallback(
    (query: string) => {
      const trimmed = query.trim();
      if (trimmed.length < minQueryLength) return;
      setIsOpen(false);
      setActiveIndex(-1);
      router.push(`/search?q=${encodeURIComponent(trimmed)}`);
    },
    [minQueryLength, router, setIsOpen],
  );

  const showPanel = isOpen && value.trim().length >= minQueryLength;

  useEffect(() => {
    setActiveIndex(-1);
  }, [items, value]);

  useEffect(() => {
    return () => {
      if (blurTimerRef.current !== null) {
        window.clearTimeout(blurTimerRef.current);
      }
    };
  }, []);

  const handleSubmit = useCallback(
    (e: FormEvent) => {
      e.preventDefault();
      if (activeIndex >= 0 && items[activeIndex]) {
        navigateToSearch(items[activeIndex].productName);
        return;
      }
      navigateToSearch(value);
    },
    [activeIndex, items, navigateToSearch, value],
  );

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (!showPanel || items.length === 0) {
      if (e.key === "Escape") {
        setIsOpen(false);
      }
      return;
    }

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((prev) => (prev + 1) % items.length);
      return;
    }

    if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((prev) => (prev <= 0 ? items.length - 1 : prev - 1));
      return;
    }

    if (e.key === "Escape") {
      e.preventDefault();
      setIsOpen(false);
      setActiveIndex(-1);
    }
  };

  const handleBlur = () => {
    blurTimerRef.current = window.setTimeout(() => {
      setIsOpen(false);
      setActiveIndex(-1);
    }, 150);
  };

  const handleFocus = () => {
    if (blurTimerRef.current !== null) {
      window.clearTimeout(blurTimerRef.current);
      blurTimerRef.current = null;
    }
    if (value.trim().length >= minQueryLength && (loading || items.length > 0)) {
      setIsOpen(true);
    }
  };

  const listboxId = "header-search-suggest-listbox";

  return (
    <form
      onSubmit={handleSubmit}
      className="relative flex w-full items-center justify-center gap-2"
    >
      <div className="relative w-full max-w-none md:max-w-[400px] lg:max-w-[600px]">
        <Input
          id="search"
          type="text"
          role="combobox"
          aria-expanded={showPanel}
          aria-controls={showPanel ? listboxId : undefined}
          aria-autocomplete="list"
          aria-activedescendant={
            showPanel && activeIndex >= 0
              ? `header-search-suggest-${activeIndex}`
              : undefined
          }
          placeholder={t("placeholder")}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={handleFocus}
          onBlur={handleBlur}
          aria-label={t("queryAriaLabel")}
          className="w-full"
          autoComplete="off"
        />

        {showPanel && (
          <ul
            id={listboxId}
            role="listbox"
            aria-label={t("suggestListAriaLabel")}
            className="absolute top-full z-50 mt-1 max-h-72 w-full overflow-y-auto rounded-md border bg-popover p-1 text-popover-foreground shadow-md"
          >
            {loading ? (
              <li className="px-3 py-2 text-sm text-muted-foreground">
                {t("suggestLoading")}
              </li>
            ) : items.length === 0 ? (
              <li className="px-3 py-2 text-sm text-muted-foreground">
                {t("suggestEmpty")}
              </li>
            ) : (
              items.map((item, index) => (
                <li
                  key={`${item.id}-${item.productName}`}
                  id={`header-search-suggest-${index}`}
                  role="option"
                  aria-selected={index === activeIndex}
                  className={cn(
                    "cursor-pointer rounded-sm px-3 py-2 text-sm",
                    index === activeIndex && "bg-accent text-accent-foreground",
                  )}
                  onMouseDown={(e) => e.preventDefault()}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => navigateToSearch(item.productName)}
                >
                  {item.productName}
                </li>
              ))
            )}
          </ul>
        )}
      </div>

      <Button type="submit" id="search-button" aria-label={t("submitAriaLabel")}>
        <Search className="h-4 w-4" id="search-icon" />
      </Button>
    </form>
  );
}
