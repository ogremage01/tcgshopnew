"use client";

import { OrderDetailDto } from "@/types/order";
import OrderItem from "@/components/OrderItem";
import { MypageOrderProductLineDto } from "@/types/order";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api";
import { useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import { Card, CardHeader, CardContent, CardFooter } from "@/components/ui/card";
export default function OrderDetail({ orderDetail }: { orderDetail: OrderDetailDto }) {

    const t = useTranslations();
    const router = useRouter();
    const handleCancelOrder = () => {
        api.post<void>(`/api/user/orders/${orderDetail.id}/cancel`, { orderId: orderDetail.id })
            .then(() => {
                alert(t("order.cancelSuccess"));
                router.push(`/mypage/order/1`);
            }).catch(() => {
                alert(t("order.cancelFailed"));
            });
    };
    return (
        <Card>
            <CardHeader>
                <h1>id: {orderDetail.id}</h1>
            </CardHeader>
            <CardContent>
                <div className="flex flex-col gap-4">
                    <h1>id: {orderDetail.id}</h1>
                    <h1>orderDate: {orderDetail.orderDate}</h1>
                    <h1>orderTotal: {orderDetail.orderTotal}</h1>
                    <h1>orderStatus: {orderDetail.orderStatus}</h1>
                    <h1>orderAmount: {orderDetail.orderAmount}</h1>
                    <h1>paymentCurrency: {orderDetail.paymentCurrency}</h1>
                    <h1>paymentStatus: {orderDetail.paymentStatus}</h1>
                    <h1>deliveryCompany: {orderDetail.deliveryCompany}</h1>
                    <h1>deliveryTrackingNumber: {orderDetail.deliveryTrackingNumber}</h1>
                    <h1>deliveryMemo: {orderDetail.deliveryMemo}</h1>
                    <h1>usedPointAmount: {orderDetail.usedPointAmount}</h1>
                    <h1>actualPaymentAmount: {orderDetail.actualPaymentAmount}</h1>
                    <h1>paymentDate: {orderDetail.paymentDate}</h1>
                </div>
                {orderDetail.orderProducts.map((orderProduct: MypageOrderProductLineDto) => (
                    <OrderItem key={orderProduct.id} item={orderProduct} />
                ))}
            </CardContent>
            <CardFooter>
                {/* <Button onClick={() => handleCancelOrder()} disabled={orderDetail.orderStatus !== "ORDER_WAITING"}>{t("order.cancelOrder")}</Button> */}
            </CardFooter>
        </Card>
    );
}