"use client";

import { ProductItemDto } from "@/types/product";
import { useLocale } from "next-intl";
import { formatCardSaleShowingPrice } from "@/utils/formatShowingPrice";
import { Separator } from "@/components/ui/separator";
import ProductQuantityButton from "@/components/productComponents/ProductQuantityButton";
import { capitalizeFirstLetter } from "@/lib/utils";
import { languageShortLabel, sortSaleMapEntries } from "@/lib/card-product-language";
import { useCartMutation } from "@/hooks/use-cart";
import { useTranslations } from "next-intl";

function isPurchasableSale(sale: {
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

export default function CardProductInfo({ productItemDto, handleSelectLanguage }: { productItemDto: ProductItemDto, handleSelectLanguage: (language: string) => void }) {
    const locale = useLocale();
    const { addCart } = useCartMutation();
    const card = productItemDto.card;
    const languageGroups = sortSaleMapEntries(card?.cardProductSaleDtoMap);
    const t = useTranslations("productDetail");
    return (
        <>
            <div className="flex flex-col gap-2">
                <div className="flex flex-col w-full bg-blue-950 text-yellow-500 p-2 gap-2">
                    <span>
                        {card?.setCode}-{card?.setNumber} ({capitalizeFirstLetter(card?.printing ?? card?.printType)})
                    </span>
                    <span>
                        {card?.setName} {capitalizeFirstLetter(card?.rarity)}
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
                        key={`${code}-${sale.searchMapId ?? sale.id ?? idx}-${sale.condition}`}
                        onClick={() => handleSelectLanguage(code)}>
                        
                        <span className="font-bold text-blue-600">{languageShortLabel(code)}</span>
                        <span> - {sale.condition}</span>
                        
                        <div className="grid grid-cols-[minmax(0,1fr)_10rem] w-full items-center gap-2">
                            <div className="grid grid-cols-6 text-sm sm:text-base gap-2 min-w-0 items-center">
                                <div className="col-span-3">
                                    {sale.currentVisibleStock > 0 ? <span className="text-xs text-gray-500 border border-gray-300 rounded-md px-1 py-1 lg:text-base"> {t("stock")}: {sale.currentVisibleStock}</span> 
                                    : <span className="text-gray-500 text-xs text-nowrap px-1 py-1 lg:text-base"> {t("stock")}: 0</span>}
                                </div>
                                <span className="col-span-3 text-xs lg:text-base">
                                    {formatCardSaleShowingPrice(locale, sale)}
                                </span>
                            </div>
                            <div
                                className="flex items-center justify-end"
                                onClick={(e) => e.stopPropagation()}
                            >
                                {isPurchasableSale(sale) ? (
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
