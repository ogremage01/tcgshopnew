"use client";

import UserOrderDetail from "@/components/order/UserOrderDetail";

type MypageOrderDetailProps = {
    orderId: string;
};

export default function MypageOrderDetail({ orderId }: MypageOrderDetailProps) {
    return <UserOrderDetail variant="member" orderId={orderId} />;
}
