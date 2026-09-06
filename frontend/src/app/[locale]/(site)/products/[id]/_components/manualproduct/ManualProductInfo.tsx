"use client";

import { ProductItemDto } from "@/types/product";
import { Separator } from "@/components/ui/separator";
import ProductQuantityButton from "@/components/productComponents/ProductQuantityButton";
import { useCartMutation } from "@/hooks/use-cart";
import { useTranslations } from "next-intl";

export default function ManualProductInfo({ productItemDto }: { productItemDto: ProductItemDto }) {
    const { addCart } = useCartMutation();
    const t = useTranslations("productDetail");
    const manual = productItemDto.manualProductInfoDto;
    const maxStock = productItemDto.currentVisibleStock ?? 0;

    if (!manual) {
        return null;
    }

    return (
        <>
            <div className="flex flex-col gap-2">
                <div className="flex flex-col w-full bg-blue-950 text-yellow-500 p-2 gap-2">
                    {productItemDto.productIp ? <span>{productItemDto.productIp}</span> : null}
                    {manual.productType ? <span>{manual.productType}</span> : null}
                </div>
                <span>{productItemDto.productNameEn}</span>
                {productItemDto.productNameKo ? <span>{productItemDto.productNameKo}</span> : null}
            </div>
            <Separator className="my-2" />
            <div className="flex flex-col gap-2 p-2">
                <div className="grid grid-cols-[minmax(0,1fr)_10rem] w-full items-center gap-2">
                    <div className="grid grid-cols-6 text-sm sm:text-base gap-2 min-w-0 items-center">
                        <span className="col-span-2 text-gray-500">
                            {t("stock")}: {maxStock}
                        </span>
                        <span className="col-span-4">
                            {productItemDto.price == null ? "—" : `₩${productItemDto.price.toLocaleString()}`}
                        </span>
                    </div>
                    <div className="flex items-center justify-end">
                        {maxStock > 0 ? (
                            <ProductQuantityButton
                                onAddToCart={(quantity) => {
                                    addCart.mutate({
                                        searchMapId: manual.searchMapId,
                                        cartUpdateRequest: { quantity },
                                    });
                                }}
                                maxStock={maxStock}
                                resetKey={manual.searchMapId}
                            />
                        ) : (
                            <span className="text-gray-500 text-sm">Out of Stock</span>
                        )}
                    </div>
                </div>
                <Separator className="my-2" />
                <div>
                    {manual.description ? (
                        <div
                            className="rich-text-editor-content"
                            dangerouslySetInnerHTML={{ __html: manual.description }}
                        />
                    ) : (
                        <span className="text-gray-500 text-sm">no text</span>
                    )}
                </div>
            </div>
        </>
    );
}
