import { CartItemDto } from "@/types/cart";
import { useLocale, useTranslations } from "next-intl";
import { formatMoneyByLocale } from "@/utils/formatShowingPrice";
import ProductImage from "@/components/product/ProductImage";
import { Button } from "@/components/ui/button";
import { Minus, Plus, Trash } from "lucide-react";
import { Input } from "@/components/ui/input";
import { ButtonGroup } from "@/components/ui/button-group";
import { Separator } from "@/components/ui/separator";
import PrintTypeLabel from "@/components/product/PrintTypeLabel";
export default function MobileList({ items, qtyOf, changeQuantity, updateCart, removeCartItem }: { items: CartItemDto[], qtyOf: (item: CartItemDto) => number, changeQuantity: (item: CartItemDto, delta: number) => void, updateCart: { isPending: boolean }, removeCartItem: { mutate: (id: number) => void, isPending: boolean } }) {
    const locale = useLocale();
    const t = useTranslations("cart");
    return (
        <div>
            {items.map((item) => (
                <div key={item.searchMapId} className="flex flex-col gap-2">
                    <div className="flex flex-row gap-2">
                        <ProductImage loading="lazy" src={item.imageUrl} alt={item.productNameEn} width={100} height={100} />
                        <div>
                            <span className="block text-ellipsis overflow-hidden whitespace-wrap">{item.productNameEn}</span>
                            <span className="block text-ellipsis overflow-hidden whitespace-wrap">{item.productNameKo}</span>
                        </div>
                    </div>
                    {item.cardProduct ? (
                        <div className="flex flex-row justify-between gap-2">
                            <div className="flex flex-row">
                                <span className="block text-ellipsis overflow-hidden whitespace-nowrap">{item.cardProduct.condition}-</span>
                                <span className="block text-ellipsis overflow-hidden whitespace-nowrap">{item.cardProduct.language}-</span>
                                <PrintTypeLabel
                                    printing={item.cardProduct.printing ?? item.cardProduct.printType}
                                    foilHighlight="badge"
                                    className="block text-ellipsis overflow-hidden whitespace-nowrap"
                                />
                            </div>
                            <span>Stock: {item.currentVisibleStock}</span>
                            <span>{formatMoneyByLocale(locale, item.price, item.priceUsd)}</span>
                        </div>
                    ) : null}
                    <div className="flex flex-row justify-between items-center gap-2">
                        <div className="flex flex-row gap-1 w-full items-center">

                            <ButtonGroup>
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="icon"
                                    disabled={qtyOf(item) <= 1 || updateCart.isPending}
                                    onClick={() => changeQuantity(item, -1)}
                                    aria-label={t("decreaseQuantity")}
                                >
                                    <Minus />
                                </Button>
                                <Input value={qtyOf(item)} className="w-12 text-center focus:outline-none focus:ring-0 focus-visible:ring-0 focus-visible:ring-offset-0" />
                                <Button
                                    type="button"
                                    variant="outline"
                                    size="icon"
                                    disabled={updateCart.isPending}
                                    onClick={() => changeQuantity(item, 1)}
                                    aria-label={t("increaseQuantity")}
                                >
                                    <Plus />
                                </Button>
                            </ButtonGroup>
                        </div>

                        <div className="flex flex-row gap-1 w-full text-center justify-end items-center">
                            <span className="blocktext-right text-lg">
                                {formatMoneyByLocale(
                                    locale,
                                    item.price * qtyOf(item),
                                    Number.isFinite(item.priceUsd)
                                        ? item.priceUsd! * qtyOf(item)
                                        : null,
                                )}
                            </span>
                            <Button variant="outline" size="icon" disabled={removeCartItem.isPending} onClick={() => removeCartItem.mutate(item.searchMapId)} aria-label="Delete item">
                                <Trash />
                            </Button>
                        </div>
                    </div>
                    <Separator className="my-4" />
                </div>
            ))}
        </div>
    );
}
