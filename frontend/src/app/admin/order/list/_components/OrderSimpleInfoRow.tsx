"use client";
import { TableRow, TableCell } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import Link from "next/link";
import { useCallback, useState } from "react";
import { AdminOrderSimpleDto } from "@/types/order";
import { createAdminOrderStatusChangeHandler } from "@/lib/admin-order-status";
import { getOrderStatusRowBadgeClass } from "@/lib/order-status";
import { getDeliveryCompanyLabelKo } from "@/lib/delivery-company";
import { AdminOrderStatusSelect } from "@/app/admin/order/_components/AdminOrderStatusSelect";
import { useAdminOrderCancelDialog } from "@/app/admin/order/_components/useAdminOrderCancelDialog";

export default function OrderSimpleInfoRow({
  order,
}: {
  order: AdminOrderSimpleDto;
}) {
  const [orderStatus, setOrderStatus] = useState(order.orderStatus);
  const { requestCancelConfirm, runWithCancelLoading, dialog } =
    useAdminOrderCancelDialog();

  const handleOrderStatusChange = useCallback(
    createAdminOrderStatusChangeHandler(
      order.id,
      () => orderStatus,
      setOrderStatus,
      requestCancelConfirm,
      { runWithCancelLoading },
    ),
    [order.id, orderStatus, requestCancelConfirm, runWithCancelLoading],
  );

  return (
    <>
      {dialog}
      <TableRow
        key={order.id}
        className={cn(getOrderStatusRowBadgeClass(orderStatus), "w-fit")}
      >
        <TableCell>{order.id}</TableCell>
        <TableCell>
          <div className="flex flex-col gap-2">
            <span>{new Date(order.orderDate).toLocaleString()}</span>
          </div>
        </TableCell>
        <TableCell>
          <div className="flex flex-col gap-2">
            <span>{order.customerName}</span>
            <span>{order.customerContact}</span>
            <span>{order.customerEmail}</span>
          </div>
        </TableCell>
        <TableCell>
          <div className="flex flex-col gap-2">
            <span>{order.orderLineCount}종</span>
            <span>{order.totalQuantity}개</span>
          </div>
        </TableCell>
        <TableCell>
          <AdminOrderStatusSelect
            value={orderStatus}
            onValueChange={handleOrderStatusChange}
            contentClassName="w-40"
          />
        </TableCell>
        <TableCell>
          <span className="text-lg font-bold">
            {getDeliveryCompanyLabelKo(order.deliveryCompany)}
          </span>
        </TableCell>
        <TableCell>
          <div className="flex flex-col gap-2">
            {/* totalPaymentAmount는 이미 상품금액 + 배송료(포인트 차감 전) */}
            <span
              className={`text-lg font-bold ${order.usedPointAmount > 0 ? "text-muted-foreground line-through" : ""}`}
            >
              ₩ {order.totalPaymentAmount.toLocaleString()}
            </span>
            {order.usedPointAmount > 0 && (
              <span className="text-lg font-bold">
                ₩ {order.actualPaymentAmount.toLocaleString()}
              </span>
            )}
          </div>
        </TableCell>
        <TableCell>
          <span className="text-lg font-bold">
            {order.paymentMethod === "카드"
              ? "CREDIT CARD"
              : order.paymentMethod}
          </span>
        </TableCell>
        <TableCell>
          <Link href={`/admin/order/detail/${order.id}`}>
            <Button size="sm">상세</Button>
          </Link>
        </TableCell>
      </TableRow>
    </>
  );
}
