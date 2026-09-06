"use client";

import { Button } from "@/components/ui/button";
import { useLocale, useTranslations } from "next-intl";
import { formatMoneyByLocale } from "@/utils/formatShowingPrice";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { CartItemDto } from "@/types/cart";
import { useEffect, useState } from "react";
import { useIsMobile } from "@/hooks/use-mobile";
import { useCartQuery, useCartMutation } from "@/hooks/use-cart";
import { Separator } from "@/components/ui/separator";
import DesktopList from "./DesktopList";
import MobileList from "./MobileList";
import { useRouter } from "@/i18n/navigation";

import { useCreateCheckoutDraftMutation } from "@/hooks/use-checkout-draft";
import { toast } from "sonner";

export default function CartClient() {
  const { data: cart } = useCartQuery();
  const { updateCart, removeCartItem, clearCart } = useCartMutation();
  const createCheckoutDraft = useCreateCheckoutDraftMutation();
  const locale = useLocale();
  const t = useTranslations("cart");
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const isMobile = useIsMobile();
  const router = useRouter();
  useEffect(() => {
    if (!cart?.cartItems) return;
    setQuantities(() => {
      const next: Record<number, number> = {};
      for (const item of cart.cartItems) {
        next[item.searchMapId] = item.quantity;
      }
      return next;
    });
  }, [cart]);

  const qtyOf = (item: CartItemDto) => quantities[item.searchMapId] ?? item.quantity;

  const cartTotalKrw =
    cart?.cartItems.reduce((acc, item) => acc + item.price * qtyOf(item), 0) ?? 0;
  const cartTotalUsd = cart?.cartItems.reduce((acc, item) => {
    const unitUsd = Number.isFinite(item.priceUsd) ? item.priceUsd! : null;
    if (unitUsd == null) return acc;
    return acc + unitUsd * qtyOf(item);
  }, 0) ?? 0;

  const changeQuantity = (item: CartItemDto, delta: number) => {
    const current = qtyOf(item);
    const next = Math.max(1, current + delta);
    updateCart.mutate(
      { id: item.searchMapId, quantity: next },
      {
        onSuccess: () => {
          setQuantities((prev) => ({ ...prev, [item.searchMapId]: next }));
        },
      },
    );
  };

  const handleClearCart = () => {
    clearCart.mutate();
  };

  if (cart?.cartItems.length === 0) {
    return (
      <div className="text-center py-8 sm:py-12">
        <h1 className="text-xl sm:text-2xl font-bold mb-4">{t("title")}</h1>
        <p className="text-muted-foreground text-sm sm:text-base">{t("empty")}</p>
      </div>
    );
  }

  return (
    <div className="my-2">
      <h1 className="text-xl sm:text-2xl font-bold mb-4 sm:mb-6">{t("title")}</h1>
      <Separator className="my-4" />
      <div className="flex flex-col lg:flex-row gap-4">
        <div className="space-y-4 w-full lg:w-3/4">
          {isMobile ?
            <MobileList items={cart?.cartItems ?? []} qtyOf={qtyOf} changeQuantity={changeQuantity} updateCart={updateCart} removeCartItem={removeCartItem} />
            : <DesktopList items={cart?.cartItems ?? []} qtyOf={qtyOf} changeQuantity={changeQuantity} updateCart={updateCart} removeCartItem={removeCartItem} />
          }
        </div>
        <div className="space-y-4 border-2 border-gray-200 pl-4 w-full h-fit p-2 lg:w-1/4">
          <h2 className="text-lg font-bold text-center">{t("total")}</h2>
          <Table>
            <TableBody>
              <TableRow>
                <TableHead>{t("products")}</TableHead>
                <TableCell className="text-end">{cart?.cartItems.length} {t("productsCount")}</TableCell>
              </TableRow>
              <TableRow>
                <TableHead>{t("quantity")}</TableHead>
                <TableCell className="text-end">
                  {cart?.cartItems.reduce((acc, item) => acc + qtyOf(item), 0)} {t("quantityCount")}
                </TableCell>
              </TableRow>
              <TableRow>
                <TableHead>{t("priceTotal")}</TableHead>
                <TableCell className="text-end">
                  {formatMoneyByLocale(locale, cartTotalKrw, cartTotalUsd)}
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
          <Button
            variant="outline"
            className="w-full"
            disabled={createCheckoutDraft.isPending}
            onClick={() => {
              createCheckoutDraft.mutate(undefined, {
                onSuccess: (res) => {
                  router.push(`/cart/checkout?draftId=${encodeURIComponent(res.publicId)}`);
                },
                onError: () => {
                  toast.error("결제를 시작할 수 없습니다. 장바구니와 재고를 확인해 주세요.");
                },
              });
            }}
          >
            {createCheckoutDraft.isPending ? "…" : t("buy")}
          </Button>
          <Button variant="destructive" className="w-full" onClick={handleClearCart}>{t("clear")}</Button>
        </div>
      </div>
    </div>
  );
}