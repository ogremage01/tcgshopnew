"use client";

import ProductQuantityButton from "../ProductQuantityButton";
import ProductListSaleRow from "../ProductListSaleRow";
import { toNonNegativeInt, toSafePositiveIntId } from "@/utils/productGuards";

type ManualProductRowProps = {
  searchMapId?: number | null;
  currentVisibleStock?: number | null;
  price?: number | null;
  handleAddToCart: (searchMapId: number, quantity: number) => void;
};

export default function ManualProductRow({
  searchMapId,
  currentVisibleStock,
  price,
  handleAddToCart,
}: ManualProductRowProps) {
  const maxStock = toNonNegativeInt(currentVisibleStock);
  const safeSearchMapId = toSafePositiveIntId(searchMapId);
  const safePrice = price != null && Number.isFinite(price) ? price : null;
  const priceLabel =
    safePrice == null ? "—" : `₩ ${safePrice.toLocaleString()}`;

  return (
    <ProductListSaleRow
      priceLabel={priceLabel}
      stock={maxStock}
      quantityButton={
        maxStock > 0 ? (
          <ProductQuantityButton
            layout="list"
            onAddToCart={(quantity) => {
              if (safeSearchMapId == null) return;
              handleAddToCart(safeSearchMapId, quantity);
            }}
            maxStock={maxStock}
            disabled={safeSearchMapId == null || maxStock < 1}
            resetKey={safeSearchMapId}
          />
        ) : undefined
      }
    />
  );
}
