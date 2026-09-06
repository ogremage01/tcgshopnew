"use client";

import { useCallback, useEffect, useState } from "react";
import { isAxiosError } from "axios";
import { apiClient, getApiErrorMessage } from "@/lib/api";
import type { Page } from "@/types/pagination";
import type {
  CardProductManagementResponseDto,
  SearchBySetDto,
} from "@/types/product";

const SEARCH_BY_SET_URL =
  "/api/admin/product/single-products/search/by-set";
const PAGE_SIZE = 20;

export function useCheckBySetSearch(searchCriteria: SearchBySetDto | null) {
  const [currentPage, setCurrentPage] = useState(1);
  const [data, setData] = useState<CardProductManagementResponseDto[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchCriteria]);

  useEffect(() => {
    if (!searchCriteria?.game || !searchCriteria.set) {
      setData([]);
      setTotalPages(0);
      setTotalElements(0);
      return;
    }

    const ac = new AbortController();
    setIsLoading(true);

    apiClient
      .get(SEARCH_BY_SET_URL, {
        params: {
          game: searchCriteria.game,
          set: searchCriteria.set,
          printTypeFilter: searchCriteria.printTypeFilter,
          storageId: searchCriteria.storageId,
          page: currentPage - 1,
          size: PAGE_SIZE,
        },
        signal: ac.signal,
      })
      .then((res) => {
        const pageData = res.data as Page<CardProductManagementResponseDto>;
        setData(pageData?.content ?? []);
        setTotalPages(pageData?.totalPages ?? 0);
        setTotalElements(pageData?.totalElements ?? 0);
      })
      .catch((err) => {
        if (isAxiosError(err) && err.code === "ERR_CANCELED") return;
        console.error(getApiErrorMessage(err) ?? err);
      })
      .finally(() => setIsLoading(false));

    return () => ac.abort();
  }, [searchCriteria, currentPage]);

  const handlePageChange = useCallback((page: number) => {
    setCurrentPage(page);
  }, []);

  const removeFromList = useCallback((id: number) => {
    setData((prev) => prev.filter((card) => card.id !== id));
    setTotalElements((count) => Math.max(0, count - 1));
  }, []);

  return {
    data,
    currentPage,
    totalPages,
    totalElements,
    isLoading,
    handlePageChange,
    removeFromList,
  };
}
