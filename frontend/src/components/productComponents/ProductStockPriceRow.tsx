"use client";

import { cn } from "@/lib/utils";
import { useTranslations } from "next-intl";

type ProductStockPriceRowProps = {
  stock: number;
  priceLabel: string;
  className?: string;
};

export default function ProductStockPriceRow({
  stock,
  priceLabel,
  className,
}: ProductStockPriceRowProps) {
  const t = useTranslations("productDetail");

  return (
    <div
      className={cn(
        "flex flex-row items-center text-lg justify-between",
        className,
      )}
    >
      <span className="text-gray-500 text-xs lg:text-sm text-nowrap border border-gray-300 rounded-md px-1 py-1">
        {t("stock")}: {stock}
      </span>
      <span className="text-xs lg:text-sm text-nowrap ml-2 border border-gray-300 rounded-md px-1 py-1">
        {priceLabel}
      </span>
    </div>
  );
}
