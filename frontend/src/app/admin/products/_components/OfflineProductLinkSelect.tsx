"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import debounce from "lodash-es/debounce";
import { XIcon } from "lucide-react";
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";
import type { OfflineProductDto } from "@/types/product";
import type { Page } from "@/types/pagination";
import { cn } from "@/lib/utils";

type OfflineProductOption = {
  value: string;
  label: string;
};

type OfflineProductLinkSelectProps = {
  value?: string;
  onValueChange: (productId: string | undefined) => void;
  disabled?: boolean;
  className?: string;
};

const SEARCH_PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

function toOfflineProductOption(product: OfflineProductDto): OfflineProductOption {
  return {
    value: product.productId,
    label: `${product.title} (${product.productId})`,
  };
}

async function fetchOfflineProductOptions(
  keyword: string,
): Promise<OfflineProductOption[]> {
  const params = {
    page: 0,
    size: SEARCH_PAGE_SIZE,
    sort: "title,asc",
    ...(keyword ? { keyword } : {}),
  };

  const page = await api.get<Page<OfflineProductDto>>(
    keyword
      ? "/api/admin/offline-data/product/search"
      : "/api/admin/offline-data/product",
    undefined,
    { params },
  );

  return page.content.map(toOfflineProductOption);
}

async function fetchOfflineProductByProductId(
  productId: string,
): Promise<OfflineProductOption> {
  const product = await api.get<OfflineProductDto>(
    "/api/admin/offline-data/product/by-product-id",
    undefined,
    { params: { productId } },
  );
  return toOfflineProductOption(product);
}

function isSameOfflineProductOption(
  a: OfflineProductOption,
  b: OfflineProductOption,
) {
  return a.value === b.value;
}

function offlineProductToLabel(item: OfflineProductOption) {
  return item.label;
}

function offlineProductToValue(item: OfflineProductOption) {
  return item.value;
}

export function OfflineProductLinkSelect({
  value,
  onValueChange,
  disabled = false,
  className,
}: OfflineProductLinkSelectProps) {
  const [open, setOpen] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [options, setOptions] = useState<OfflineProductOption[]>([]);
  const [linkedProduct, setLinkedProduct] = useState<OfflineProductOption | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState(false);
  const resolvedProductIdRef = useRef<string | null>(null);

  const debouncedSetSearchQuery = useMemo(
    () =>
      debounce((query: string) => {
        setSearchQuery(query);
      }, SEARCH_DEBOUNCE_MS),
    [],
  );

  useEffect(() => {
    if (!value) {
      resolvedProductIdRef.current = null;
      setLinkedProduct(null);
      return;
    }

    if (resolvedProductIdRef.current === value) {
      return;
    }

    let cancelled = false;
    void fetchOfflineProductByProductId(value)
      .then((option) => {
        if (!cancelled) {
          resolvedProductIdRef.current = value;
          setLinkedProduct(option);
        }
      })
      .catch((error) => {
        console.error(error);
        if (!cancelled) {
          resolvedProductIdRef.current = value;
          setLinkedProduct({ value, label: value });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [value]);

  useEffect(() => {
    if (!open) {
      debouncedSetSearchQuery.cancel();
      return;
    }

    debouncedSetSearchQuery(searchInput.trim());
    return () => {
      debouncedSetSearchQuery.cancel();
    };
  }, [searchInput, open, debouncedSetSearchQuery]);

  useEffect(() => {
    if (!open) return;

    let cancelled = false;

    setIsLoading(true);
    void fetchOfflineProductOptions(searchQuery)
      .then((nextOptions) => {
        if (!cancelled) {
          setOptions(nextOptions);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          console.error(error);
          setOptions([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [searchQuery, open]);

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);

    if (nextOpen) {
      setSearchInput("");
      setSearchQuery("");
    }
  };

  const handleSearchSelect = (option: OfflineProductOption | null) => {
    if (!option) return;

    resolvedProductIdRef.current = option.value;
    setLinkedProduct(option);
    onValueChange(option.value);
    setSearchInput("");
    setSearchQuery("");
    setOpen(false);
  };

  const handleClearLink = () => {
    resolvedProductIdRef.current = null;
    setLinkedProduct(null);
    onValueChange(undefined);
  };

  return (
    <div className={cn("flex flex-col gap-3", className)}>
      <Label>오프라인 상품 연동</Label>

      <div className="flex flex-col gap-1.5">
        <span className="text-xs text-muted-foreground">연동된 상품</span>
        <div className="flex gap-2">
          <Input
            readOnly
            tabIndex={-1}
            value={linkedProduct?.label ?? ""}
            placeholder="연동된 오프라인 상품 없음"
            disabled={disabled}
            className="bg-muted/30"
          />
          {value ? (
            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={handleClearLink}
              disabled={disabled}
              aria-label="오프라인 상품 연동 해제"
            >
              <XIcon className="size-4" />
            </Button>
          ) : null}
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <span className="text-xs text-muted-foreground">상품 검색</span>
        <Combobox<OfflineProductOption>
          items={options}
          onValueChange={handleSearchSelect}
          open={open}
          onOpenChange={handleOpenChange}
          inputValue={searchInput}
          onInputValueChange={setSearchInput}
          disabled={disabled}
          filter={null}
          isItemEqualToValue={isSameOfflineProductOption}
          itemToStringLabel={offlineProductToLabel}
          itemToStringValue={offlineProductToValue}
        >
          <ComboboxInput
            placeholder="오프라인 상품 검색"
            disabled={disabled}
            showClear
          />
          <ComboboxContent className="bg-white p-0">
            <ComboboxEmpty>
              {isLoading ? "검색 중..." : "검색 결과가 없습니다."}
            </ComboboxEmpty>
            <ComboboxList className="max-h-60 overflow-y-auto overscroll-contain">
              {(item) => (
                <ComboboxItem key={item.value} value={item}>
                  {item.label}
                </ComboboxItem>
              )}
            </ComboboxList>
          </ComboboxContent>
        </Combobox>
      </div>
    </div>
  );
}
