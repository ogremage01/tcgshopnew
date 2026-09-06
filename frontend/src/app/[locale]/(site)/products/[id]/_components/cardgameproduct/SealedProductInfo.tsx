"use client";

import { ProductItemDto } from "@/types/product";
import { Separator } from "@/components/ui/separator";
import ProductQuantityButton from "@/components/productComponents/ProductQuantityButton";
import { languageShortLabel, sortSaleMapEntries } from "@/lib/card-product-language";
import { useCartMutation } from "@/hooks/use-cart";

function isPurchasableSealedSale(sale: {
    id?: number | null
    searchMapId?: number | null
    currentVisibleStock: number
}) {
    return (
        sale.id != null &&
        sale.searchMapId != null &&
        sale.currentVisibleStock > 0
    );
}

export default function SealedProductInfo({ productItemDto, handleSelectLanguage }: { productItemDto: ProductItemDto, handleSelectLanguage?: (language: string) => void }) {
    const { addCart } = useCartMutation();
    const languageGroups = sortSaleMapEntries(productItemDto.sealedProductInfoDto?.sealedProductSaleDtoMap);

    return (
        <>
            <div className="flex flex-col gap-2">
                <div className="flex flex-col w-full bg-blue-950 text-yellow-500 p-2 gap-2">
                    <span>
                        {productItemDto.setName} ({productItemDto.setCode})
                    </span>
                    <span>
                        {productItemDto.sealedProductInfoDto?.game}
                    </span>
                </div>
                <span>{productItemDto.productNameEn}</span>
                {productItemDto.productNameKo ? <span>{productItemDto.productNameKo}</span> : null}
            </div>
            <Separator className="my-2" />
            {languageGroups.map(({ code, sales }) =>
                sales.map((sale, idx) => (
                    <div
                        className="hover:bg-gray-200 transition-colors duration-200 p-2 gap-2"
                        key={`${code}-${sale.searchMapId ?? sale.id ?? idx}`}
                        onClick={() => handleSelectLanguage?.(code)}>
                        <span>{languageShortLabel(code)}</span>
                        <div className="grid grid-cols-[minmax(0,1fr)_10rem] w-full items-center gap-2">
                            <div className="grid grid-cols-6 text-sm sm:text-base gap-2 min-w-0 items-center">
                                <span className="col-span-2">
                                    {sale.currentVisibleStock > 0 ? <span>{sale.currentVisibleStock}</span> 
                                    : <span className="text-gray-500">0</span>}
                                </span>
                                <span className="col-span-4">
                                    ₩{sale.showingPrice.toLocaleString()}
                                </span>
                            </div>
                            <div
                                className="flex items-center justify-end"
                                onClick={(e) => e.stopPropagation()}
                            >
                                {isPurchasableSealedSale(sale) ? (
                                    <ProductQuantityButton
                                        onAddToCart={(quantity) => {
                                            if (sale.searchMapId == null) return;
                                            addCart.mutate({
                                                searchMapId: sale.searchMapId,
                                                cartUpdateRequest: { quantity },
                                            });
                                        }}
                                        maxStock={sale.currentVisibleStock}
                                        resetKey={sale.searchMapId}
                                    />
                                ) : null}
                            </div>
                        </div>
                        <Separator />
                    </div>
                ))
            )}
        </>
    )
}
