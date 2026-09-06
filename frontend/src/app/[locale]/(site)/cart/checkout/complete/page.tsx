"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";

import { Link } from "@/i18n/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function CheckoutCompletePage() {
  const searchParams = useSearchParams();
  const orderId = searchParams?.get("orderId") ?? "";
  const guestCode = searchParams?.get("guestCode");
  const t = useTranslations("checkoutComplete");

  return (
    <div className="w-full my-8 max-w-lg mx-auto space-y-6">
      <h1 className="text-2xl font-bold">{t("title")}</h1>

      <Card>
        <CardHeader>
          <CardTitle>{t("orderInfo")}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <div className="flex justify-between gap-4">
            <span className="text-muted-foreground">{t("orderNumber")}</span>
            <span className="font-mono">{orderId || "—"}</span>
          </div>
          {guestCode ? (
            <div className="flex justify-between gap-4">
              <span className="text-muted-foreground">{t("guestConfirmCode")}</span>
              <span className="font-mono tracking-widest">{guestCode}</span>
            </div>
          ) : null}
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2">
        <Button asChild variant="default">
          <Link href="/">{t("goHome")}</Link>
        </Button>
        <Button asChild variant="outline">
          <Link href="/cart">{t("cart")}</Link>
        </Button>
      </div>
    </div>
  );
}
