"use client";

import type { ProductItemDto } from "@/types/product";
import { resolveAssetUrl } from "@/lib/public-asset-url";
import ProductQuantityButton from "../ProductQuantityButton";
import ProductStockPriceRow from "../ProductStockPriceRow";
import GridProductImage from "./GridProductImage";
import Link from "next/link";

export default function ManualProduct({ productItemDto, handleAddToCart }: { productItemDto: ProductItemDto, handleAddToCart: (searchMapId: number, quantity: number) => void }) {
    const manual = productItemDto.manualProductInfoDto;
    const imageSrc = resolveAssetUrl(productItemDto.imageUrlEn);
    if (!manual) {
        return null;
    }
    return (
        <div className="flex h-full flex-col gap-2">
            <div className="flex flex-col gap-1">
                <span className="text-gray-500 text-sm text-ellipsis overflow-hidden whitespace-nowrap">{productItemDto.productIp}</span>
                <span className="text-gray-500 text-sm text-ellipsis overflow-hidden whitespace-nowrap">{productItemDto.manualProductInfoDto?.productType}</span>
                <div className="image-container relative w-full overflow-hidden rounded-lg bg-gray-100 aspect-[65/90]">
                <Link href={`/products/${productItemDto.manualProductInfoDto?.publicId}`}>
                    <GridProductImage
                        src={imageSrc}
                        alt={productItemDto.productNameEn}
                    />
                </Link>
                </div>
            </div>
            <div className="flex flex-col gap-1">
            <ProductStockPriceRow
                stock={productItemDto.currentVisibleStock}
                priceLabel={
                    productItemDto.price == null
                        ? "—"
                        : `₩ ${productItemDto.price.toLocaleString()}`
                }
            />
            {productItemDto.currentVisibleStock > 0 && (
                <ProductQuantityButton onAddToCart={(quantity) => handleAddToCart(manual.searchMapId, quantity)} maxStock={productItemDto.currentVisibleStock} disabled={productItemDto.currentVisibleStock < 1} layout="grid" />
            )}
            </div>
        </div>
    )
}
