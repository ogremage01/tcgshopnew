import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from "@/components/ui/table";
import { ButtonGroup } from "@/components/ui/button-group";
import { Minus, Plus, Trash } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CartItemDto } from "@/types/cart";
import { useLocale, useTranslations } from "next-intl";
import { formatMoneyByLocale } from "@/utils/formatShowingPrice";
import ProductImage from "@/components/product/ProductImage";
import PrintTypeLabel from "@/components/product/PrintTypeLabel";

export default function DesktopList({ items, qtyOf, changeQuantity, updateCart, removeCartItem }: { items: CartItemDto[], qtyOf: (item: CartItemDto) => number, changeQuantity: (item: CartItemDto, delta: number) => void, updateCart: { isPending: boolean }, removeCartItem: { mutate: (id: number) => void, isPending: boolean } }) {
    const locale = useLocale();
    const t = useTranslations("cart");
    return (
        <Table className="table-fixed">
            <TableHeader>
                <TableRow>
                    <TableHead className="w-[100px] text-center">{t("image")}</TableHead>
                    <TableHead className="w-max-[30%] min-w-0 text-center">{t("productInfo")}</TableHead>
                    <TableHead className="text-center">{t("price")}</TableHead>
                    <TableHead className="w-[200px] text-center">{t("quantity")}</TableHead>
                    <TableHead className="w-[180px] text-center">{t("total")}</TableHead>
                </TableRow>
            </TableHeader>

            <TableBody>
                {items.map((item) => (
                    <TableRow key={item.searchMapId}>
                        <TableCell id="image" className="w-[100px] align-top">
                            <ProductImage loading="lazy" src={item.imageUrl} alt={item.productNameEn} width={100} height={100} />
                        </TableCell>
                        <TableCell id="productName" className="min-w-0 w-[18%] max-w-[18%] align-top">
                            <div className="min-w-0 max-w-full">
                                {item.cardProduct ? (
                                    <div className="flex flex-row">
                                        <span className="block text-ellipsis overflow-hidden whitespace-nowrap">{item.cardProduct.condition}-{item.cardProduct.language}-</span>
                                        <PrintTypeLabel
                                            printing={item.cardProduct.printing ?? item.cardProduct.printType}
                                            foilHighlight="badge"
                                            className="block text-ellipsis overflow-hidden whitespace-nowrap"
                                        />
                                    </div>
                                ) : null}
                                <span className="block text-ellipsis overflow-hidden whitespace-nowrap">{item.productNameEn}</span>
                                <span className="block text-ellipsis overflow-hidden whitespace-nowrap">{item.productNameKo}</span>
                            </div>
                        </TableCell>
                        <TableCell id="price" className="text-end">
                            {formatMoneyByLocale(locale, item.price, item.priceUsd)}
                        </TableCell>
                        <TableCell id="quantity">
                            <div className="flex flex-col items-center gap-1">
                                <div className="flex flex-row gap-1">

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
                                <span className="text-gray-500 text-lg">{t("stock")}: {item.currentVisibleStock}</span>
                            </div>
                        </TableCell>
                        <TableCell id="total">
                            <div className="grid grid-cols-2 gap-1 w-full items-center justify-end text-end">
                                <span className="text-end">
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
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table >
    );
}