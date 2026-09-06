"use client"
import type { ProductItemDto } from "@/types/product";
import SealedProductRow from "./SealedProductRow";
import ProductImage from "@/components/product/ProductImage";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import Link from "next/link";
import { languageImageCandidates, languageShortLabel, sortSaleMapEntries } from "@/lib/card-product-language";

export default function SealedProduct({ productItemDto, handleAddToCart }: {
    productItemDto: ProductItemDto,
    handleAddToCart: (searchMapId: number, quantity: number) => void
}) {
    const languageGroups = sortSaleMapEntries(productItemDto.sealedProductInfoDto?.sealedProductSaleDtoMap);
    const firstLanguage = languageGroups[0]?.code ?? "en";
    const imageCandidates = languageImageCandidates(
        productItemDto.languageImageUrlMap,
        firstLanguage,
        productItemDto.imageUrlEn,
    );

    return (
        <>
            <div className="flex flex-col">
                <div className="flex flex-col">
                    <div className="flex flex-row">
                        <ul className="flex flex-row flex-wrap gap-2">
                            <li className="text-gray-500 text-sm">{productItemDto?.sealedProductInfoDto?.game}</li>
                            <li className="text-gray-500 text-sm">/ {productItemDto?.setName}</li>
                        </ul>
                    </div>
                </div>
                <div className="w-full justify-between gap-2 rounded-md bg-gray-100 flex flex-col sm:flex-row">
                    <div className="image-container relative w-full sm:w-[180px] max-w-[260px] sm:max-w-none mx-auto sm:mx-0 shrink-0 overflow-hidden rounded-lg bg-gray-100 aspect-[63/88]">
                        <Link href={`/products/${productItemDto.sealedProductInfoDto?.publicId}`}>
                            <ProductImage
                                src={imageCandidates[0]}
                                fallbackSrcs={imageCandidates.slice(1)}
                                alt={productItemDto.productNameEn}
                                fill
                                sizes="(max-width: 640px) 70vw, 180px"
                                className="object-contain object-top"
                            />
                        </Link>
                    </div>
                    <div className="flex flex-row flex-1 min-w-0 gap-2">
                        <div className="flex flex-col gap-2 w-full">
                            {languageGroups.map(({ code, sales }) => (
                                <div key={code} className="flex flex-row items-center">
                                    <div className="text-left flex flex-col w-full">
                                        <Label className="text-gray-500 my-2 font-bold">{languageShortLabel(code)}</Label>
                                        {sales.map((sale, idx) => (
                                            <div
                                                key={sale.id ?? `unavailable-${idx}`}
                                                className="w-full min-w-0 transition-colors duration-200 hover:bg-gray-200"
                                            >
                                                {sale.currentVisibleStock > 0 ? (
                                                    <SealedProductRow
                                                        sale={sale}
                                                        handleAddToCart={handleAddToCart}
                                                    />
                                                ) : (
                                                    <div className="flex justify-end items-center p-2">
                                                        <span className="text-gray-500">Out of Stock</span>
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                        <Separator className="my-1" />
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div >
        </>
    )
}
