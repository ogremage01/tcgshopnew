"use client";

import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { ShoppingCart } from "lucide-react";
import { ScrollArea } from "../ui/scroll-area";
import { Separator } from "../ui/separator";
import { useCartQuery, useCartMutation } from "@/hooks/use-cart";
import { useRouter } from "@/i18n/navigation";
import { useLocale, useTranslations } from "next-intl";
import { formatMoneyByLocale } from "@/utils/formatShowingPrice";
import { languageShortLabel } from "@/lib/card-product-language";
import ProductImage from "../product/ProductImage";
import PrintTypeLabel from "../product/PrintTypeLabel";
import { X } from "lucide-react";
import WindowPortal from "@/components/WindowPortal";
export default function Cart() {
  const { data: cart } = useCartQuery();
  const { clearCart } = useCartMutation();
  const router = useRouter();
  const moveToCart = () => {
    router.push("/cart/");
  };
  const locale = useLocale();
  const t = useTranslations("cart");
  return (
    <WindowPortal>
      <Sheet>
      <div className="fixed top-40 right-4 z-50 pointer-events-auto">
        <SheetTrigger asChild>
          <Button variant="outline" className="shadow-md">
            <ShoppingCart />
          </Button>
        </SheetTrigger>
      </div>
      <SheetContent>
        <SheetHeader>
          <SheetTitle>
            <div className="flex flex-row items-center gap-2">
              <ShoppingCart />
              <span className="text-2xl font-bold">{t("cartList")}</span>
            </div>
          </SheetTitle>
        </SheetHeader>
        <Separator className="my-2" />
        <ScrollArea className="h-[calc(100vh-200px)] w-full min-w-0 [&>[data-radix-scroll-area-viewport]>div]:!block [&>[data-radix-scroll-area-viewport]>div]:!min-w-0">
          {cart?.cartItems.length === 0 || !cart ? (
            <div className="items-left justify-center gap-4 py-4">
              <span className="text-2xl font-bold block text-center">
                {t("cartEmpty")}
              </span>
            </div>
          ) : (
            cart?.cartItems.map((item) => (
              <div className="w-full min-w-0 overflow-hidden" key={item.id}>
                <div className="grid w-full min-w-0 grid-cols-[minmax(0,1fr)_auto] items-center gap-2">
                  <div className="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] items-center gap-2 overflow-hidden">
                    <div className="shrink-0">
                      <ProductImage
                        src={item.imageUrl}
                        alt={item.productNameEn}
                        width={70}
                        height={70}
                      />
                    </div>
                    <div className="min-w-0 overflow-hidden text-xs md:text-sm">
                      {item.cardProduct ? (
                        <>
                          <span className="block truncate">
                            {item.cardProduct.setName}
                          </span>
                          <div className="flex min-w-0 items-center overflow-hidden">
                            <span className="truncate">
                              {item.cardProduct.condition}-
                              {item.cardProduct.language
                                ? languageShortLabel(item.cardProduct.language)
                                : null}
                              -
                            </span>
                            <PrintTypeLabel
                              printing={item.cardProduct.printing ?? item.cardProduct.printType}
                              foilHighlight="badge"
                              className="truncate"
                            />
                          </div>
                          <span className="block truncate">
                            {[
                              item.cardProduct.setCode,
                              item.cardProduct.setNumber,
                            ]
                              .filter(Boolean)
                              .join("-")}
                          </span>
                        </>
                      ) : null}
                      <span className="block truncate">
                        {item.productNameEn}
                      </span>
                      <span className="block truncate">
                        {item.productNameKo}
                      </span>
                      <div className="mt-1 flex flex-row flex-wrap items-center gap-1.5 md:hidden">
                        <span className="text-xs whitespace-nowrap">
                          {formatMoneyByLocale(
                            locale,
                            item.price,
                            Number.isFinite(item.priceUsd)
                              ? item.priceUsd!
                              : null,
                          )}
                        </span>
                        <div className="flex flex-row items-center gap-1 text-xs">
                          <X className="h-3 w-3" />
                          {item.quantity}
                        </div>
                        <span className="text-xs">=</span>
                        <span className="text-xs whitespace-nowrap">
                          {formatMoneyByLocale(
                            locale,
                            item.price * item.quantity,
                            Number.isFinite(item.priceUsd)
                              ? item.priceUsd! * item.quantity
                              : null,
                          )}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="hidden shrink-0 flex-col items-end gap-2 md:flex">
                    <span className="text-sm whitespace-nowrap">
                      {formatMoneyByLocale(
                        locale,
                        item.price,
                        Number.isFinite(item.priceUsd) ? item.priceUsd! : null,
                      )}
                    </span>
                    <div className="flex flex-row items-center gap-2 text-sm">
                      <X className="h-4 w-4" />
                      {item.quantity}
                    </div>
                    <Separator />
                    <p className="text-sm whitespace-nowrap">
                      {formatMoneyByLocale(
                        locale,
                        item.price * item.quantity,
                        Number.isFinite(item.priceUsd)
                          ? item.priceUsd! * item.quantity
                          : null,
                      )}
                    </p>
                  </div>
                </div>
                <Separator className="my-1" />
              </div>
            ))
          )}
        </ScrollArea>
        <div className="flex flex-row justify-center items-center gap-2 my-2">
          <SheetClose asChild>
            <Button variant="outline" onClick={moveToCart}>
              {t("buy")}
            </Button>
          </SheetClose>
          <Button variant="destructive" onClick={() => clearCart.mutate()}>
            {t("clear")}
          </Button>
        </div>
      </SheetContent>
      </Sheet>
    </WindowPortal>
  );
}
