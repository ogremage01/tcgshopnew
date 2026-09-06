"use client";
import type { CardProductSaleDto } from "@/types/product";
import { toNonNegativeInt, toSafePositiveIntId } from "@/utils/productGuards";
import ProductQuantityButton from "../ProductQuantityButton";
import ProductListSaleRow from "../ProductListSaleRow";
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
  const priceLabel = formatCardSaleShowingPrice(locale, sale);
  const conditionLabel =
    typeof sale?.condition === "string" && sale.condition.trim() !== ""
      ? sale.condition
      : "—";
  const searchMapId = toSafePositiveIntId(sale?.searchMapId);

  return (
    <ProductListSaleRow
      leadingLabel={conditionLabel}
      priceLabel={priceLabel}
      stock={sale.currentVisibleStock}
      quantityButton={
        maxStock > 0 ? (
          <ProductQuantityButton
            layout="list"
            onAddToCart={(quantity) => {
              if (searchMapId == null) return;
              handleAddToCart(searchMapId, quantity);
            }}
            maxStock={maxStock}
            disabled={searchMapId == null}
            resetKey={searchMapId}
          />
        ) : undefined
      }
    />
  );
}
