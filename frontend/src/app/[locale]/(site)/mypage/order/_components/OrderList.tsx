"use client";

import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "@/i18n/navigation";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import PaginationRange from "@/components/ui/pagination-range";
import { OrderSimpleDto } from "@/types/order";

interface Props {
  initialOrders: OrderSimpleDto[];
  totalPages: number;
  currentPage: number;
}

export default function OrderList({ initialOrders, totalPages, currentPage }: Props) {
  const router = useRouter();
  const locale = useLocale();
  const t = useTranslations();
  const to = useTranslations("order");

  return (
    <div className="flex flex-col gap-4 p-4">
      {initialOrders.length > 0 ? (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{to("listColumns.orderId")}</TableHead>
              <TableHead>{to("listColumns.orderDate")}</TableHead>
              <TableHead>{to("listColumns.orderTotal")}</TableHead>
              <TableHead>{to("listColumns.orderStatus")}</TableHead>
              <TableHead>{to("listColumns.orderAmount")}</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {initialOrders.map((order) => (
              <TableRow
                key={order.id}
                onClick={() => router.push(`/mypage/order/detail/${order.id}`)}
                className="cursor-pointer"
              >
                <TableCell>{order.id}</TableCell>
                <TableCell>{order.orderDate.toLocaleString()}</TableCell>
                <TableCell>{order.orderTotal}</TableCell>
                <TableCell>{to(`status.${order.orderStatus}` as never)}</TableCell>
                <TableCell>₩ {order.orderAmount.toLocaleString()}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      ) : (
        <div className="flex flex-col items-center justify-center h-full">
          <span>{t("order.empty")}</span>
        </div>
      )}
      <PaginationRange
        currentPage={currentPage}
        totalPages={totalPages}
        siblingCount={2}
        hrefForPage={(page) => `/${locale}/mypage/order/${page}`}
      />
    </div>
  );
}
