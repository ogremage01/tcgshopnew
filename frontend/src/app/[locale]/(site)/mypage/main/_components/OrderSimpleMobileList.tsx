"use client";

import { useLocale, useTranslations } from "next-intl";
import { Separator } from "@/components/ui/separator";
import type { OrderSimpleDto } from "@/types/order";
import { formatOrderTotal } from "./format-order-total";
import OrderRowActions from "./OrderRowActions";

type OrderSimpleMobileListProps = {
    orders: OrderSimpleDto[];
    className?: string;
};

export default function OrderSimpleMobileList({ orders, className }: OrderSimpleMobileListProps) {
    const to = useTranslations("order");
    const locale = useLocale();

    return (
        <div className={className ?? "block md:hidden"}>
            <div className="flex flex-col border p-2 gap-2">
                {orders.map((order) => (
                    <div className="border p-2 gap-2" key={order.id}>
                        <div className="flex flex-col gap-2">
                            <span>
                                {to("listColumns.orderId")}: {order.id}
                            </span>
                            <span>
                                {to("listColumns.orderDate")}: {new Date(order.orderDate).toLocaleString(locale)}
                            </span>
                            <span>
                                {to("listColumns.orderTotal")}:{" "}
                                {formatOrderTotal(order.orderTotal, order.paymentCurrency)}
                            </span>
                            <span>
                                {to("listColumns.orderStatus")}: {to(`status.${order.orderStatus}` as never)}
                            </span>
                        </div>
                        <Separator className="my-2" />
                        <OrderRowActions order={order} className="flex flex-row gap-2 justify-end items-center" />
                    </div>
                ))}
            </div>
        </div>
    );
}
