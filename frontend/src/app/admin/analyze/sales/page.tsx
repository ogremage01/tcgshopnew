"use client";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
} from "@/components/ui/table";
import { useState, useEffect } from "react";
import { api } from "@/lib/api";
import { AdminDailySalesSummarySimpleDto } from "@/types/order";
import { Page } from "@/types/pagination";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";

const PAGE_SIZE = 20;

export default function AdminAnalyzeSalesPage() {
  const [dailySalesSummary, setDailySalesSummary] = useState<
    AdminDailySalesSummarySimpleDto[]
  >([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    void api
      .get<Page<AdminDailySalesSummarySimpleDto>>(
        "/api/admin/analyze/daily-sales-summary",
        undefined,
        {
          params: {
            page: currentPage - 1,
            size: PAGE_SIZE,
          },
        },
      )
      .then((pageData) => {
        setDailySalesSummary(pageData.content ?? []);
        setTotalPages(Math.max(pageData.totalPages ?? 0, 1));
      })
      .catch((error) => {
        console.error(error);
        setDailySalesSummary([]);
        setTotalPages(1);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [currentPage]);

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          온라인 주문 일별 매출 현황(임시 페이지. 6/17~현재)
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 gap-4">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>주문일</TableHead>
              <TableHead className="text-right">주문 건수(취소 제외)</TableHead>
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
            {loading ? (
              <TableRow>
                <TableCell colSpan={6} className="h-24 text-center">
                  불러오는 중...
                </TableCell>
              </TableRow>
            ) : dailySalesSummary.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="h-24 text-center text-muted-foreground"
                >
                  조회된 매출 데이터가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              dailySalesSummary.map((row) => (
                <TableRow key={row.orderDate}>
                  <TableCell>{row.orderDate}</TableCell>
                  <TableCell className="text-right">
                    {row.totalOrderCount.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    {`₩${row.totalOrderAmount.toLocaleString()}`}
                  </TableCell>
                  <TableCell className="text-right">
                    {`₩${row.totalUsedPointAmount.toLocaleString()}`}
                  </TableCell>
                  <TableCell className="text-right">
                    {`₩${row.totalDeliveryFee.toLocaleString()}`}
                  </TableCell>
                  <TableCell className="text-right">
                    {`₩${row.totalActualPaymentAmount.toLocaleString()}`}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <PaginationRangeAsync
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </CardContent>
    </Card>
  );
}
