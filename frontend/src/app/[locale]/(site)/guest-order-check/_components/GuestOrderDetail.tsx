"use client";

import UserOrderDetail from "@/components/order/UserOrderDetail";

type GuestOrderDetailProps = {
    orderId: string;
    guestCode?: string;
};

export default function GuestOrderDetail({ orderId, guestCode }: GuestOrderDetailProps) {
    return <UserOrderDetail variant="guest" orderId={orderId} guestCode={guestCode} />;
}
