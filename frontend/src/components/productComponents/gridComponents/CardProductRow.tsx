"use client";
import type { CardProductSaleDto } from "@/types/product";
import { toNonNegativeInt, toSafePositiveIntId } from "@/utils/productGuards";
import ProductQuantityButton from "../ProductQuantityButton";
import ProductStockPriceRow from "../ProductStockPriceRow";
import { useLocale } from "next-intl";
import { formatCardSaleShowingPrice } from "@/utils/formatShowingPrice";

export default function CardProductRow({
  sale,
  handleAddToCart,
  rewardPercentage,
}: {
  sale: CardProductSaleDto;
  handleAddToCart: (searchMapId: number, quantity: number) => void;
  rewardPercentage?: number | null;
}) {
  const locale = useLocale();
  const maxStock = toNonNegativeInt(sale?.currentVisibleStock);
  const safePrice = Number.isFinite(sale?.showingPrice)
    ? sale.showingPrice
    : null;
  const priceLabel = formatCardSaleShowingPrice(locale, sale);
  const conditionLabel =
    typeof sale?.condition === "string" && sale.condition.trim() !== ""
      ? sale.condition
      : "—";
  const searchMapId = toSafePositiveIntId(sale?.searchMapId);
  const pts =
    safePrice != null && rewardPercentage
      ? Math.floor(safePrice * rewardPercentage)
      : null;
  return (
    <div className="flex flex-col gap-1 my-2">
      <ProductStockPriceRow stock={sale.currentVisibleStock} priceLabel={priceLabel} />

      {maxStock > 0 && (
        <div className="flex flex-row flex-nowrap justify-between items-center">
          <ProductQuantityButton
            onAddToCart={(quantity) => {
              if (searchMapId == null) return;
              handleAddToCart(searchMapId, quantity);
            }}
            maxStock={maxStock}
            disabled={searchMapId == null}
            resetKey={searchMapId}
            layout="grid"
          />
        </div>
      )}
    </div>
  );
}
