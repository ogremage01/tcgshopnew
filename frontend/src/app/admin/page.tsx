"use client";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
  TableFooter,
} from "@/components/ui/table";

import { AdminSalesSummarySimpleDto } from "@/types/order";
import { useState, useEffect } from "react";
import { api } from "@/lib/api";
import CardStockTable from "./_components/CardStockTable";
export default function AdminPage() {
  const [salesSummary, setSalesSummary] =
    useState<AdminSalesSummarySimpleDto | null>(null);
  useEffect(() => {
    const fetchSalesSummary = async () => {
      const response = await api.get<AdminSalesSummarySimpleDto>(
        "/api/admin/analyze/current-month-sales-summary",
      );
      setSalesSummary(response);
    };
    fetchSalesSummary();
  }, []);
  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">대시보드</h2>

      <Card>
        <CardHeader>
          <CardTitle>당월 매출 현황</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>주문 건수(취소 제외)</TableHead>
                <TableHead className="text-right">주문 금액</TableHead>
                <TableHead className="text-right">
                  주문 포인트 사용 금액
                </TableHead>
                <TableHead className="text-right">주문 배송료 금액</TableHead>
                <TableHead className="text-right">
                  주문 실결제 금액(배송료 포함)
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {salesSummary && (
                <TableRow>
                  <TableCell>
                    {salesSummary.totalOrderCount.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    ₩{salesSummary.totalOrderAmount.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    ₩{salesSummary.totalUsedPointAmount.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    ₩{salesSummary.totalDeliveryFee.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    ₩{salesSummary.totalActualPaymentAmount.toLocaleString()}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
      <CardStockTable />
    </div>
  );
}
