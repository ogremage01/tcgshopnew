"use client";

import { useCallback } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

export function useProductFilterParams() {
  const router = useRouter();
  const pathname = usePathname() ?? "";
  const searchParams = useSearchParams();

  const setArrayParam = useCallback(
    (key: string, values: string[]) => {
      const next = new URLSearchParams(searchParams?.toString() ?? "");
      if (values.length === 0) next.delete(key);
      else next.set(key, values.join(","));
      next.set("page", "0");
      next.set("entryState", "UPDATED");
      router.push(`${pathname}?${next.toString()}`);
    },
    [pathname, router, searchParams],
  );

  const toggleArrayValue = useCallback(
    (key: string, current: string[], value: string, checked: boolean) => {
      const nextValues = checked
        ? [...current.filter((v) => v !== value), value]
        : current.filter((v) => v !== value);
      setArrayParam(key, nextValues);
    },
    [setArrayParam],
  );

  const setBooleanParam = useCallback(
    (key: string, checked: boolean) => {
      const next = new URLSearchParams(searchParams?.toString() ?? "");
      if (checked) next.set(key, "true");
      else next.delete(key);
      next.set("page", "0");
      next.set("entryState", "UPDATED");
      router.push(`${pathname}?${next.toString()}`);
    },
    [pathname, router, searchParams],
  );

  const setNullableBooleanParam = useCallback(
    (key: string, value: boolean | null) => {
      const next = new URLSearchParams(searchParams?.toString() ?? "");
      if (value === null) next.delete(key);
      else next.set(key, String(value));
      next.set("page", "0");
      next.set("entryState", "UPDATED");
      router.push(`${pathname}?${next.toString()}`);
    },
    [pathname, router, searchParams],
  );

  const resetFilters = useCallback(() => {
    router.push(`${pathname}?entryState=INITIAL`);
  }, [pathname, router]);

  return {
    setArrayParam,
    toggleArrayValue,
    setBooleanParam,
    setNullableBooleanParam,
    resetFilters,
  };
}
