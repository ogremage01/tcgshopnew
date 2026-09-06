"use client";

import type { ProductItemDto } from "@/types/product";
import SealedProduct from "./SealedProduct";
import SupplyProduct from "./SupplyProduct";
import CardProduct from "./CardProduct";
import ManualProduct from "./ManualProduct";
import { useCartMutation } from "@/hooks/use-cart";
import { useLocale, useTranslations } from "next-intl";

export default function ProductListItem({ product }: { product: ProductItemDto }) {
    const { addCart } = useCartMutation();
    const t = useTranslations("game");
    const locale = useLocale();
    const displayName =
        locale === "ko"
            ? product?.productNameKo || t("noKoreanInfo")
            : product?.productNameEn;
    const handleAddToCart = (searchMapId: number, quantity: number) => {
        //console.log(searchMapId, quantity);
        addCart.mutate({ searchMapId: searchMapId, cartUpdateRequest: { quantity: quantity } });
    }

    const productType = product.productType;

    const inner =
        productType === "SealedProducts" ? (
            <SealedProduct productItemDto={product} handleAddToCart={handleAddToCart} />
        ) :
            productType === "Supplies" ? (
                <SupplyProduct productItemDto={product} handleAddToCart={handleAddToCart} />
            ) : productType === "Cards" ? (
                <CardProduct productItemDto={product} handleAddToCart={handleAddToCart} />
            ) : productType === "ManualProducts" ? (
                <ManualProduct productItemDto={product} handleAddToCart={handleAddToCart} />
            ) : null;

    if (inner === null) {
        return null;
    }
    return <div className="my-2">
        <div className="flex flex-col md:flex-row gap-2">
            {productType === "ManualProducts" ? (
                <span className="text-ellipsis overflow-hidden whitespace-nowrap">{displayName}</span>
            ) : (
                <>
                    <span className="text-ellipsis overflow-hidden whitespace-nowrap">{product?.productNameEn}</span>
                    <span className="hidden md:inline">-</span>
                    <span className="text-ellipsis overflow-hidden whitespace-nowrap">{product?.productNameKo ? `${product?.productNameKo}` : t("noKoreanInfo")}</span>
                </>
            )}
        </div>
        {inner}
    </div>;
}