"use client";

import type { OrderSimpleDto } from "@/types/order";
import OrderSimpleDesktopTable from "./OrderSimpleDesktopTable";
import OrderSimpleMobileList from "./OrderSimpleMobileList";

type OrderSimpleListProps = {
    orders: OrderSimpleDto[];
    desktopClassName?: string;
    mobileClassName?: string;
};

export default function OrderSimpleList({
    orders,
    desktopClassName,
    mobileClassName,
}: OrderSimpleListProps) {
    return (
        <>
            <OrderSimpleDesktopTable orders={orders} className={desktopClassName} />
            <OrderSimpleMobileList orders={orders} className={mobileClassName} />
        </>
    );
}
