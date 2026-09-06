"use client";

import type { ProductItemDto } from "@/types/product";
import ProductImage from "@/components/product/ProductImage";
import { resolveAssetUrl } from "@/lib/public-asset-url";
import { getSupplyProductTypeDisplayName } from "@/lib/supplies-type-label";
import { useLocale } from "next-intl";
import SupplyProductRow from "./SupplyProductRow";

export default function SupplyProduct({
  productItemDto,
  handleAddToCart,
}: {
  productItemDto: ProductItemDto;
  handleAddToCart: (searchMapId: number, quantity: number) => void;
}) {
  const supply = productItemDto.supplyProductInfoDto;
  const locale = useLocale();
  const imageSrc = resolveAssetUrl(productItemDto.imageUrlEn);

  if (!supply) {
    return null;
  }

  const maxStock = productItemDto.currentVisibleStock ?? 0;

  return (
    <div className="flex flex-col">
      <div className="flex flex-row">
        <ul className="flex flex-row flex-wrap gap-2">
          <li className="text-gray-500 text-sm">{supply.maker}</li>
          <li className="text-gray-500 text-sm">/ {getSupplyProductTypeDisplayName(supply, locale)}</li>
        </ul>
      </div>
      <div className="w-full justify-between gap-2 rounded-md bg-gray-100 flex flex-col sm:flex-row">
        <div className="image-container relative w-full sm:w-[180px] max-w-[260px] sm:max-w-none mx-auto sm:mx-0 shrink-0 overflow-hidden rounded-lg bg-gray-100 aspect-[65/90]">
          <ProductImage
            src={imageSrc}
            alt={productItemDto.productNameEn}
            fill
            sizes="(max-width: 640px) 70vw, 180px"
            className="object-contain object-top"
          />
        </div>
        <div className="flex flex-row flex-1 min-w-0 gap-2">
          <div className="flex flex-col gap-2 w-full">
            <div className="w-full min-w-0 transition-colors duration-200 hover:bg-gray-200">
              {maxStock > 0 ? (
                <SupplyProductRow
                  searchMapId={supply.searchMapId}
                  currentVisibleStock={productItemDto.currentVisibleStock}
                  price={productItemDto.price}
                  handleAddToCart={handleAddToCart}
                />
              ) : (
                <div className="flex justify-end items-center p-2">
                  <span className="text-gray-500">Out of Stock</span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
