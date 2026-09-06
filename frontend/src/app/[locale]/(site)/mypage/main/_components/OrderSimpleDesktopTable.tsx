"use client";

import { useLocale, useTranslations } from "next-intl";
import {
    Table,
    TableHeader,
    TableBody,
    TableRow,
    TableCell,
    TableHead,
} from "@/components/ui/table";
import type { OrderSimpleDto } from "@/types/order";
import { formatOrderTotal } from "./format-order-total";
import OrderRowActions from "./OrderRowActions";

type OrderSimpleDesktopTableProps = {
    orders: OrderSimpleDto[];
    className?: string;
};

export default function OrderSimpleDesktopTable({ orders, className }: OrderSimpleDesktopTableProps) {
    const to = useTranslations("order");
    const locale = useLocale();

    return (
        <div className={className ?? "hidden md:block w-full"}>
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>{to("listColumns.orderId")}</TableHead>
                        <TableHead>{to("listColumns.orderDate")}</TableHead>
                        <TableHead>{to("listColumns.orderTotal")}</TableHead>
                        <TableHead>{to("listColumns.orderStatus")}</TableHead>
                        <TableHead />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {orders.map((order) => (
                        <TableRow key={order.id}>
                            <TableCell>{order.id}</TableCell>
                            <TableCell>{new Date(order.orderDate).toLocaleString(locale)}</TableCell>
                            <TableCell>
                                {formatOrderTotal(order.orderTotal, order.paymentCurrency)}
                            </TableCell>
                            <TableCell>{to(`status.${order.orderStatus}` as never)}</TableCell>
                            <TableCell>
                                <OrderRowActions order={order} />
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    );
}
