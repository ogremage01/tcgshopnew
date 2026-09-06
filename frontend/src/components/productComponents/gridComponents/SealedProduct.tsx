"use client";

import type { ProductItemDto, SealedProductSaleDto } from "@/types/product";
import { useMemo, useState } from "react";
import SealedProductRow from "./SealedProductRow";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import Link from "next/link";
import ProductImage from "@/components/product/ProductImage";
import { PRODUCT_GRID_IMAGE_SIZES } from "@/constants/productImage";
import {
    compareLanguageCode,
    languageImageCandidates,
    languageShortLabel,
    sortSaleMapEntries,
} from "@/lib/card-product-language";
import { Separator } from "@/components/ui/separator";

type SealedSaleWithLanguage = SealedProductSaleDto & { languageCode: string };

export default function SealedProduct({ productItemDto, handleAddToCart }:
    {
        productItemDto: ProductItemDto,
        handleAddToCart: (searchMapId: number, quantity: number) => void,
    }) {
    const [selectedLanguage, setSelectedLanguage] = useState<string | null>(null);
    const [selectedSaleId, setSelectedSaleId] = useState<number | null>(null);
    const languageGroups = useMemo(
        () => sortSaleMapEntries(productItemDto.sealedProductInfoDto?.sealedProductSaleDtoMap),
        [productItemDto.sealedProductInfoDto?.sealedProductSaleDtoMap],
    );
    const salesByLanguage = useMemo(
        () => Object.fromEntries(languageGroups.map((group) => [group.code, group.sales])),
        [languageGroups],
    );
    const defaultSale = useMemo<SealedSaleWithLanguage | null>(
        () =>
            languageGroups
                .flatMap(({ code, sales }) => sales.map((sale) => ({ ...sale, languageCode: code })))
                .sort((a, b) => {
                    const aHasStock = a.currentVisibleStock > 0 ? 0 : 1;
                    const bHasStock = b.currentVisibleStock > 0 ? 0 : 1;
                    if (aHasStock !== bHasStock) return aHasStock - bHasStock;
                    return compareLanguageCode(a.languageCode, b.languageCode);
                })[0] ?? null,
        [languageGroups],
    );
    const activeLanguage = selectedLanguage ?? defaultSale?.languageCode ?? null;
    const salesForSelectedLanguage = useMemo(
        () => activeLanguage ? (salesByLanguage[activeLanguage] ?? []) : [],
        [activeLanguage, salesByLanguage],
    );
    const selectedItem = useMemo(
        () => {
            if (selectedSaleId != null) {
                const selected = salesForSelectedLanguage.find((sale) => sale.id === selectedSaleId);
                if (selected) return selected;
            }
            if (activeLanguage === defaultSale?.languageCode) return defaultSale;
            return salesForSelectedLanguage[0] ?? null;
        },
        [activeLanguage, defaultSale, salesForSelectedLanguage, selectedSaleId],
    );
    const imageCandidates = languageImageCandidates(
        productItemDto.languageImageUrlMap,
        activeLanguage,
        productItemDto.imageUrlEn,
    );
    const validImageCandidates = imageCandidates.filter((c): c is string => Boolean(c));

    const handleSelectLanguage = (language: string) => {
        if (!language) return;
        const sales = salesByLanguage[language];
        if (!sales || sales.length === 0) return;
        setSelectedLanguage(language);
        setSelectedSaleId(sales[0]?.id ?? null);
    }

    return (
        <div className="flex h-full flex-col">
            <div className="flex h-full flex-col gap-1">
                <span className="text-gray-500 text-sm">{productItemDto.sealedProductInfoDto?.game}</span>
                <span className="text-gray-500 text-sm truncate">{productItemDto.setName}</span>
                <div className="image-container relative w-full overflow-hidden bg-white rounded-lg aspect-[63/88]">
                    <Link href={`/products/${productItemDto.sealedProductInfoDto?.publicId}`} className="absolute inset-0">
                        <ProductImage
                            src={validImageCandidates[0]}
                            fallbackSrcs={validImageCandidates.slice(1)}
                            alt={productItemDto.productNameEn}
                            fill
                            sizes={PRODUCT_GRID_IMAGE_SIZES}
                            className="object-contain object-top"
                        />
                    </Link>
                </div>
                <div className="flex flex-col flex-1 min-w-0 gap-2">
                    <div className="flex flex-col flex-1 w-full">
                        <ToggleGroup
                            type="single"
                            value={activeLanguage ?? ""}
                            onValueChange={handleSelectLanguage}
                            className="justify-start"
                        >
                            {languageGroups.map(({ code }) => (
                                <ToggleGroupItem
                                key={code}
                                value={code}
                                className="min-w-7 shrink-0 px-1 py-0 text-xs leading-4">
                                    {languageShortLabel(code)}
                                </ToggleGroupItem>
                            ))}
                        </ToggleGroup>
                        <Separator className="my-1" />
                        {selectedItem && (
                            <SealedProductRow
                                sale={selectedItem}
                                handleAddToCart={handleAddToCart}
                            />
                        )}
                    </div>
                </div>
            </div>
        </div >
    );
}
