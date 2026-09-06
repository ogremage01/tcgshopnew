"use client";

import { useEffect, useState } from "react";
import { isAxiosError } from "axios";
import { apiClient, getApiErrorMessage } from "@/lib/api";
import type { ProductSuggestItem, ProductSuggestResponse } from "@/types/product";

const DEBOUNCE_MS = 300;
const MIN_QUERY_LENGTH = 2;
const SUGGEST_LIMIT = 10;

export function useHeaderSearchSuggest(query: string) {
  const [items, setItems] = useState<ProductSuggestItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed.length < MIN_QUERY_LENGTH) {
      setItems([]);
      setLoading(false);
      setIsOpen(false);
      return;
    }

    const ac = new AbortController();
    setLoading(true);

    const timer = window.setTimeout(() => {
      apiClient
        .get<ProductSuggestResponse>("/api/products/suggest", {
          params: { q: trimmed, limit: SUGGEST_LIMIT },
          signal: ac.signal,
        })
        .then((res) => {
          setItems(res.data.items ?? []);
          setIsOpen(true);
        })
        .catch((err) => {
          if (isAxiosError(err) && err.code === "ERR_CANCELED") return;
          console.error(getApiErrorMessage(err) ?? err);
          setItems([]);
        })
        .finally(() => {
          if (!ac.signal.aborted) {
            setLoading(false);
          }
        });
    }, DEBOUNCE_MS);

    return () => {
      window.clearTimeout(timer);
      ac.abort();
    };
  }, [query]);

  return {
    items,
    loading,
    isOpen,
    setIsOpen,
    minQueryLength: MIN_QUERY_LENGTH,
  };
}
