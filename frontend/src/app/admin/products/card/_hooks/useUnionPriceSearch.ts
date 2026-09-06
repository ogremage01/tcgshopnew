"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { apiClient, getApiErrorMessage } from "@/lib/api";
import type { Page } from "@/types/pagination";
import type { UnionPriceSlimDto } from "@/types/product";

export const ADMIN_CARD_CATALOG_SEARCH_CARDS_URL =
  "/api/admin/product/single-products/search/cards";

const PAGE_SIZE = 12;

export type UnionPriceSearchFilters = {
  game?: string;
  setCode?: string;
};

export type UnionPriceSetFacet = {
  setCode: string;
  setName?: string | null;
};

export type UnionPriceGameSetFacet = {
  game: string;
  sets: UnionPriceSetFacet[];
};

type UnionPriceAdminSearchResponse = {
  cards: Page<UnionPriceSlimDto>;
  facets: UnionPriceGameSetFacet[];
};

export function useUnionPriceSearch(
  keyword: string,
  filters: UnionPriceSearchFilters = {},
) {
  const [currentPage, setCurrentPage] = useState(1);
  const [data, setData] = useState<UnionPriceSlimDto[]>([]);
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
      .get(ADMIN_CARD_CATALOG_SEARCH_CARDS_URL, {
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
        const body = res.data as UnionPriceAdminSearchResponse;
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

  const hasKeyword = keyword.trim().length > 0;
  return {
    data: hasKeyword ? data : [],
    currentPage,
    totalPages: hasKeyword ? totalPages : 0,
    totalElements: hasKeyword ? totalElements : 0,
    facets: hasKeyword ? facets : [],
    handlePageChange,
  };
}
