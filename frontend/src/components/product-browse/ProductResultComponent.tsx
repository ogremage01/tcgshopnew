"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  ProductGridItem,
  ProductListItem,
} from "@/components/productComponents";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import type { PageResponse, ProductItemDto } from "@/types/product";
import { getProductListItemKey } from "@/utils/productGuards";
import { Separator } from "@/components/ui/separator";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { LayoutGrid, LayoutList } from "lucide-react";
import { useTranslations } from "next-intl";
type Props = {
  products: PageResponse<ProductItemDto>;
  sortBy: string;
  size: string;
};

export default function ProductResultComponent({
  products,
  sortBy,
  size,
}: Props) {
  const LAYOUT_STORAGE_KEY = "search-result-layout";
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [selectedSortBy, setSelectedSortBy] = useState<string>(sortBy);
  const [selectedSize, setSelectedSize] = useState<string>(size);
  const [layout, setLayout] = useState<"grid" | "list" | null>(null);
  const [isLayoutReady, setIsLayoutReady] = useState(false);
  const prevPageRef = useRef(products.number);
  const t = useTranslations("productBrowse.sort");
  useEffect(() => {
    const savedLayout = localStorage.getItem(LAYOUT_STORAGE_KEY);
    if (savedLayout === "grid" || savedLayout === "list") {
      setLayout(savedLayout);
    } else {
      setLayout("grid");
    }
    setIsLayoutReady(true);
  }, []);

  useEffect(() => {
    if (layout) {
      localStorage.setItem(LAYOUT_STORAGE_KEY, layout);
    }
  }, [layout]);

  useEffect(() => {
    if (prevPageRef.current !== products.number) {
      window.scrollTo({ top: 0, behavior: "smooth" });
      prevPageRef.current = products.number;
    }
  }, [products.number]);

  const handleLayoutChange = (value: "grid" | "list") => {
    setLayout(value);
  };

  const handlePageChange = useCallback(
    (page: number) => {
      window.scrollTo({ top: 0, behavior: "smooth" });
      const next = new URLSearchParams(searchParams?.toString() ?? "");
      next.set("page", String(page - 1));
      next.set("entryState", "UPDATED");
      router.push(`${pathname}?${next.toString()}`);
    },
    [pathname, router, searchParams],
  );

  const handleSortByChange = useCallback(
    (value: string) => {
      setSelectedSortBy(value);
      const next = new URLSearchParams(searchParams?.toString() ?? "");
      next.set("sortBy", value);
      next.set("page", "0");
      next.set("entryState", "UPDATED");
      router.push(`${pathname}?${next.toString()}`);
    },
    [pathname, router, searchParams],
  );

  const handleSizeChange = useCallback(
    (value: string) => {
      setSelectedSize(value);
      const next = new URLSearchParams(searchParams?.toString() ?? "");
      next.set("size", value);
      next.set("page", "0");
      next.set("entryState", "UPDATED");
      router.push(`${pathname}?${next.toString()}`);
    },
    [pathname, router, searchParams],
  );

  return (
    <div className="w-full lg:w-3/4">
      <div className="my-2 flex flex-row gap-2 items-center justify-end">
        <Select value={selectedSortBy} onValueChange={handleSortByChange}>
          <SelectTrigger className="w-[180px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="0">{t("setnumber")}</SelectItem>
            <SelectItem value="1">{t("alpha")}</SelectItem>
            <SelectItem value="2">{t("alphaReverse")}</SelectItem>
            <SelectItem value="3">{t("highToLow")}</SelectItem>
            <SelectItem value="4">{t("lowToHigh")}</SelectItem>
          </SelectContent>
        </Select>
        <Select value={selectedSize} onValueChange={handleSizeChange}>
          <SelectTrigger className="w-[180px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="24">24</SelectItem>
            <SelectItem value="36">36</SelectItem>
            <SelectItem value="48">48</SelectItem>
            <SelectItem value="60">60</SelectItem>
          </SelectContent>
        </Select>
        <Button
          variant="outline"
          className={`w-10 h-10 ${layout === "grid" ? "bg-white" : "bg-gray-200"}`}
          onClick={() => handleLayoutChange("grid")}
          disabled={!isLayoutReady || layout === "grid"}
          aria-label={t("gridLayout")}
        >
          <LayoutGrid />
        </Button>
        <Button
          variant="outline"
          className={`w-10 h-10 ${layout === "list" ? "bg-white" : "bg-gray-200"}`}
          onClick={() => handleLayoutChange("list")}
          disabled={!isLayoutReady || layout === "list"}
          aria-label={t("listLayout")}
        >
          <LayoutList />
        </Button>
      </div>
      <Separator />
      <div>
        {!isLayoutReady || layout === null ? (
          <div className="min-h-[320px]" />
        ) : layout === "grid" ? (
          <div className="grid grid-cols-2 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-3 xl:grid-cols-5 2xl:grid-cols-6 gap-4 auto-rows-fr items-stretch">
            {products.content.map((product, index) => (
              <ProductGridItem key={getProductListItemKey(product, index)} product={product} />
            ))}
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            {products.content.map((product, index) => (
              <ProductListItem key={getProductListItemKey(product, index)} product={product} />
            ))}
          </div>
        )}
        <PaginationRangeAsync
          currentPage={products.number + 1}
          totalPages={products.totalPages}
          onPageChange={handlePageChange}
        />
      </div>
    </div>
  );
}
