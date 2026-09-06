"use client";

import type { ProductItemDto } from "@/types/product";
import SealedProduct from "./SealedProduct";
import SupplyProduct from "./SupplyProduct";
import CardProduct from "./CardProduct";
import ManualProduct from "./ManualProduct";

import { useLocale, useTranslations } from "next-intl";
import { useCartMutation } from "@/hooks/use-cart";
export default function ProductGridItem({
  product,
}: {
  product: ProductItemDto;
}) {
  const t = useTranslations("game");
  const locale = useLocale();
  const { addCart } = useCartMutation();
  const displayName =
    locale === "ko"
      ? product?.productNameKo || t("noKoreanInfo")
      : product?.productNameEn;
  const handleAddToCart = (searchMapId: number, quantity: number) => {
    addCart.mutate({
      searchMapId: searchMapId,
      cartUpdateRequest: { quantity: quantity },
    });
  };

  const productType = product.productType;

  const inner =
    productType === "SealedProducts" ? (
      <SealedProduct
        productItemDto={product}
        handleAddToCart={handleAddToCart}
      />
    ) : productType === "Supplies" ? (
      <SupplyProduct
        productItemDto={product}
        handleAddToCart={handleAddToCart}
      />
    ) : productType === "Cards" ? (
      <CardProduct productItemDto={product} handleAddToCart={handleAddToCart} />
    ) : productType === "ManualProducts" ? (
      <ManualProduct
        productItemDto={product}
        handleAddToCart={handleAddToCart}
      />
    ) : null;

  if (inner === null) {
    return null;
  }
  return (
    <div className="my-2 flex h-full flex-col">
      <div className="flex min-h-12 flex-col">
        {productType === "ManualProducts" ? (
          <span className="text-ellipsis overflow-hidden whitespace-nowrap">
            {displayName}
          </span>
        ) : (
          <>
            <span className="text-ellipsis overflow-hidden whitespace-nowrap">
              {product?.productNameEn}
            </span>
            <span className="text-ellipsis overflow-hidden whitespace-nowrap">
              {product?.productNameKo
                ? `${product?.productNameKo}`
                : t("noKoreanInfo")}
            </span>
          </>
        )}
      </div>
      <div className="flex-1">{inner}</div>
    </div>
  );
}
