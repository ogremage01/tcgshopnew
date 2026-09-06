"use client";

import { Separator } from "@/components/ui/separator";
import { useTranslations } from "next-intl";
import type { ReactNode } from "react";

type ProductListSaleRowProps = {
  priceLabel: string;
  stock: number;
  leadingLabel?: string;
  quantityButton?: ReactNode;
};

export default function ProductListSaleRow({
  priceLabel,
  stock,
  leadingLabel,
  quantityButton,
}: ProductListSaleRowProps) {
  const t = useTranslations("productDetail");

  return (
    <>
      <Separator />
      <div className="flex w-full min-w-0 flex-col gap-2 px-2 py-1 sm:flex-row sm:items-center">
        <div className="flex min-w-0 flex-1 items-center gap-x-2 overflow-hidden text-sm sm:text-base">
          {leadingLabel != null && (
            <span className="w-8 shrink-0 font-medium">{leadingLabel}</span>
          )}
          <span className="min-w-0 flex-1 truncate">{priceLabel}</span>
          <span className="shrink-0 whitespace-nowrap text-xs text-gray-500 sm:text-sm">
            {t("stock")}: {stock}
          </span>
        </div>
        {quantityButton != null && (
          <div className="flex shrink-0 items-center justify-end">
            {quantityButton}
          </div>
        )}
      </div>
    </>
  );
}
