"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { apiClient, getApiErrorMessage } from "@/lib/api";
import type { Page } from "@/types/pagination";
import type { CardProductManagementResponseDto } from "@/types/product";
import type {
  UnionPriceGameSetFacet,
  UnionPriceSearchFilters,
} from "./useUnionPriceSearch";

const ADMIN_CARD_CATALOG_SEARCH_PRODUCTS_URL =
  "/api/admin/product/single-products/search/products";

const PAGE_SIZE = 12;

type CardProductAdminSearchResponse = {
  cards: Page<CardProductManagementResponseDto>;
  facets: UnionPriceGameSetFacet[];
};

export function useProductManagementSearch(
  keyword: string,
  filters: UnionPriceSearchFilters = {},
) {
  const [currentPage, setCurrentPage] = useState(1);
  const [data, setData] = useState<CardProductManagementResponseDto[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [facets, setFacets] = useState<UnionPriceGameSetFacet[]>([]);

  const isFirstRender = useRef(true);

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    setCurrentPage(1);
  }, [keyword, filters.game, filters.setCode]);

  useEffect(() => {
    const kw = keyword.trim();
    if (!kw) {
      setData([]);
      setTotalPages(0);
      setTotalElements(0);
      setFacets([]);
      return;
    }

    const ac = new AbortController();
    apiClient
      .get(ADMIN_CARD_CATALOG_SEARCH_PRODUCTS_URL, {
        params: {
          keyword: kw,
          page: currentPage - 1,
          size: PAGE_SIZE,
          ...(filters.game ? { game: filters.game } : {}),
          ...(filters.setCode ? { setCode: filters.setCode } : {}),
        },
        signal: ac.signal,
      })
      .then((res) => {
        const body = res.data as CardProductAdminSearchResponse;
        const pageData = body?.cards;
        setData(pageData?.content ?? []);
        setTotalPages(pageData?.totalPages ?? 0);
        setTotalElements(pageData?.totalElements ?? 0);
        setFacets(body?.facets ?? []);
      })
      .catch((err) => {
        if (isAxiosError(err) && err.code === "ERR_CANCELED") return;
        console.error(getApiErrorMessage(err) ?? err);
      });

    return () => ac.abort();
  }, [filters.game, filters.setCode, keyword, currentPage]);

  const handlePageChange = useCallback((page: number) => {
    setCurrentPage(page);
  }, []);

  const removeFromList = useCallback((id: number) => {
    setData((prev) => prev.filter((c) => c.id !== id));
    setTotalElements((n) => Math.max(0, n - 1));
  }, []);

  return {
    data: keyword.trim() ? data : [],
    currentPage,
    totalPages: keyword.trim() ? totalPages : 0,
    totalElements: keyword.trim() ? totalElements : 0,
    facets: keyword.trim() ? facets : [],
    handlePageChange,
    removeFromList,
  };
}
