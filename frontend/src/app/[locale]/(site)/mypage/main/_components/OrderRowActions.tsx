"use client";

import { useTranslations } from "next-intl";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/ui/button";
import type { OrderSimpleDto } from "@/types/order";

type OrderRowActionsProps = {
    order: OrderSimpleDto;
    className?: string;
};

export default function OrderRowActions({ order, className }: OrderRowActionsProps) {
    const to = useTranslations("order");
    const t = useTranslations();

    return (
        <div className={className ?? "flex flex-row gap-2 justify-end items-center"}>
            <Link href={`/mypage/order/detail/${order.id}`} target="_blank">
                <Button variant="outline" size="sm">
                    {t("common.detail")}
                </Button>
            </Link>
            {/* {order.orderStatus === "ORDER_COMPLETED" ? (
                <Button variant="destructive" size="sm">
                    {t("common.cancel")}
                </Button>
            ) : (
                <span className="text-sm text-muted-foreground">{to("cancelNotAllowed")}</span>
            )} */}
        </div>
    );
}
