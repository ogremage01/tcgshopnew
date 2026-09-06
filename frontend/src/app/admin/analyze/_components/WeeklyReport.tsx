"use client";

import { useEffect, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { api } from "@/lib/api";
import { AdminWeeklySalesReportRowDto } from "@/types/order";
import { formatCurrency } from "@/app/admin/offline/sales/_lib/payment-source-type";
import { Page } from "@/types/pagination";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";

const PAGE_SIZE = 20;

function formatWeekLabel(monday: string): string {
  const start = new Date(monday);
  const end = new Date(monday);
  end.setDate(end.getDate() + 6);
  const fmt = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  return `${fmt(start)} ~ ${fmt(end)}`;
}

export default function WeeklyReport() {
  const [rows, setRows] = useState<AdminWeeklySalesReportRowDto[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRows = (page: number) => {
    setLoading(true);
    api
      .get<Page<AdminWeeklySalesReportRowDto>>(
        `/api/admin/analyze/weekly-sales-report-list?page=${page - 1}&size=${PAGE_SIZE}`,
      )
      .then((res) => {
        setRows(res.content ?? []);
        setTotalPages(res.totalPages ?? 0);
        setError(null);
      })
      .catch(() => {
        setError("주간 매출 보고 목록을 불러오지 못했습니다.");
        setRows([]);
      })
      .finally(() => setLoading(false));
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  useEffect(() => {
    fetchRows(currentPage);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage]);

  return (
    <div>
      <Card>
        <CardHeader>
          <CardTitle>주간 매출 보고</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>주간 기간</TableHead>
                <TableHead className="text-right">총 매출(온라인 + 오프라인-직접 결제)</TableHead>
                <TableHead className="text-right">온라인 매출</TableHead>
                <TableHead className="text-right">오프라인 매출</TableHead>
                <TableHead>상세 보기</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {error ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="text-center text-destructive"
                  >
                    {error}
                  </TableCell>
                </TableRow>
              ) : loading ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center">
                    Loading...
                  </TableCell>
                </TableRow>
              ) : rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center">
                    매출 데이터가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((row) => (
                  <TableRow key={row.monday}>
                    <TableCell>{formatWeekLabel(row.monday)}</TableCell>
                    <TableCell className="text-right">
                      {formatCurrency(row.totalAmount)}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatCurrency(row.onlineAmount)}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatCurrency(row.offlineAmount)}
                    </TableCell>
                    <TableCell>
                      <Button variant="outline" asChild>
                        <Link
                          href={`/admin/analyze/sales-report/weekly/${row.monday}`}
                        >
                          상세 보기
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          <PaginationRangeAsync
            currentPage={currentPage}
            totalPages={totalPages}
            siblingCount={2}
            onPageChange={handlePageChange}
          />
        </CardContent>
      </Card>
    </div>
  );
}
