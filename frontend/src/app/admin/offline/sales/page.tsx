"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { apiClient } from "@/lib/api.client";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import {
  OfflineSalesSummaryDto,
  OfflineSalesSummaryPeriod,
} from "@/types/sales";
import { Page } from "@/types/pagination";
import {
  PERIOD_OPTIONS,
  buildReportHref,
  formatPeriodLabel,
} from "./_lib/period";

const PAGE_SIZE = 10;

function getRowKey(
  period: OfflineSalesSummaryPeriod,
  summary: OfflineSalesSummaryDto,
  index: number,
): string {
  return summary.periodStart ?? `${period}-total-${index}`;
}

export default function AdminOfflineSalesPage() {
  const [period, setPeriod] = useState<OfflineSalesSummaryPeriod>("DAILY");
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [salesSummaryList, setSalesSummaryList] = useState<
    OfflineSalesSummaryDto[]
  >([]);

  useEffect(() => {
    void apiClient
      .get<Page<OfflineSalesSummaryDto>>(
        "/api/admin/offline-data/sales/summary",
        {
          params: {
            period,
            page: currentPage - 1,
            size: PAGE_SIZE,
          },
        },
      )
      .then((response) => {
        const pageData = response.data;
        setSalesSummaryList(pageData.content);
        setTotalPages(Math.max(pageData.totalPages, 1));
      })
      .catch((error) => {
        console.error(error);
      });
  }, [period, currentPage]);

  const handlePeriodChange = (nextPeriod: OfflineSalesSummaryPeriod) => {
    setPeriod(nextPeriod);
    setCurrentPage(1);
  };

  return (
    <div>
      <Card>
        <CardHeader>
          <CardTitle>오프라인 매출 현황(오프라인 싱글카드 결제 포함)</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-row flex-wrap gap-2">
            {PERIOD_OPTIONS.map((option) => (
              <Button
                key={option.value}
                variant={period === option.value ? "default" : "outline"}
                onClick={() => handlePeriodChange(option.value)}
              >
                {option.label}
              </Button>
            ))}
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>기간</TableHead>
                <TableHead className="text-right">주문 건수</TableHead>
                <TableHead className="text-right">주문 금액</TableHead>
                <TableHead className="text-right">할인 금액</TableHead>
                <TableHead className="text-right">세액</TableHead>
                <TableHead className="text-right">공급가액</TableHead>
                <TableHead className="text-right">면세 금액</TableHead>
                <TableHead className="text-right">총액</TableHead>
                <TableHead>보고서</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {salesSummaryList.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="h-24 text-center text-muted-foreground"
                  >
                    조회된 매출 데이터가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                salesSummaryList.map((summary, index) => (
                  <TableRow key={getRowKey(period, summary, index)}>
                    <TableCell>
                      {formatPeriodLabel(period, summary.periodStart)}
                    </TableCell>
                    <TableCell className="text-right">
                      {summary.totalOrderCount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{summary.totalOrderAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{summary.totalDiscountAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{summary.totalTaxAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{summary.totalSupplyAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{summary.totalTaxExemptAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{summary.totalTotalAmount.toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <Button variant="outline" size="sm" asChild>
                        <Link
                          href={buildReportHref(period, summary.periodStart)}
                        >
                          보고서
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          {period !== "TOTAL" && (
            <PaginationRangeAsync
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
