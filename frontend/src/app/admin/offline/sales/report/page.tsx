"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import { ArrowLeft } from "lucide-react";
import { apiClient } from "@/lib/api.client";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { OfflineSalesSummaryDto } from "@/types/sales";
import SalesSummaryItemsTable from "../_components/SalesSummaryItemsTable";
import SalesSummaryPaymentsTable from "../_components/SalesSummaryPaymentsTable";
import {
  formatPeriodLabel,
  getPeriodOptionLabel,
  isOfflineSalesSummaryPeriod,
} from "../_lib/period";

function formatCurrency(value: number): string {
  return `₩${value.toLocaleString()}`;
}

function SummaryMetric({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-lg border bg-background p-4">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-1 text-lg font-semibold">{value}</p>
    </div>
  );
}

export default function AdminOfflineSalesReportPage() {
  const searchParams = useSearchParams();
  const periodParam = searchParams?.get("period") ?? null;
  const periodStart = searchParams?.get("periodStart") ?? null;
  const [report, setReport] = useState<OfflineSalesSummaryDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const isValidPeriod = isOfflineSalesSummaryPeriod(periodParam);
  const needsPeriodStart = periodParam !== "TOTAL";
  const isValidRequest =
    isValidPeriod && (!needsPeriodStart || periodStart !== null);

  useEffect(() => {
    if (!isValidRequest || !periodParam) {
      setIsLoading(false);
      setErrorMessage("잘못된 보고서 요청입니다.");
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    void apiClient
      .get<OfflineSalesSummaryDto>(
        "/api/admin/offline-data/sales/summary/report",
        {
          params: {
            period: periodParam,
            ...(periodStart ? { periodStart } : {}),
          },
        },
      )
      .then((response) => {
        setReport(response.data);
      })
      .catch(() => {
        setReport(null);
        setErrorMessage("보고서 데이터를 불러오지 못했습니다.");
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [isValidRequest, periodParam, periodStart]);

  if (!isValidRequest || !periodParam) {
    return (
      <ReportShell periodLabel="오프라인 매출 보고서">
        <p className="text-muted-foreground">{errorMessage}</p>
      </ReportShell>
    );
  }

  const periodLabel = formatPeriodLabel(periodParam, periodStart);

  if (isLoading) {
    return (
      <ReportShell periodLabel={`${periodLabel} 보고서`}>
        <p className="text-muted-foreground">Loading...</p>
      </ReportShell>
    );
  }

  if (errorMessage || !report) {
    return (
      <ReportShell periodLabel={`${periodLabel} 보고서`}>
        <p className="text-muted-foreground">
          {errorMessage ?? "조회된 매출 데이터가 없습니다."}
        </p>
      </ReportShell>
    );
  }

  return (
    <ReportShell periodLabel={`${periodLabel} 보고서`}>
      <Card>
        <CardHeader>
          <CardTitle>매출 요약</CardTitle>
          <CardDescription>
            {getPeriodOptionLabel(periodParam)} · {periodLabel}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <SummaryMetric
              label="주문 건수"
              value={report.totalOrderCount.toLocaleString()}
            />
            <SummaryMetric
              label="주문 금액"
              value={formatCurrency(report.totalOrderAmount)}
            />
            <SummaryMetric
              label="할인 금액"
              value={formatCurrency(report.totalDiscountAmount)}
            />
            <SummaryMetric
              label="세액"
              value={formatCurrency(report.totalTaxAmount)}
            />
            <SummaryMetric
              label="공급가액"
              value={formatCurrency(report.totalSupplyAmount)}
            />
            <SummaryMetric
              label="면세 금액"
              value={formatCurrency(report.totalTaxExemptAmount)}
            />
            <SummaryMetric
              label="총액"
              value={formatCurrency(report.totalTotalAmount)}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>결제 수단별 집계</CardTitle>
          <CardDescription>
            완료된 주문 기준 결제 수단별 금액 합계입니다.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <SalesSummaryPaymentsTable payments={report.payments ?? []} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>상품별 집계</CardTitle>
          <CardDescription>
            카테고리를 클릭하면 상품 목록을 펼칠 수 있습니다.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <SalesSummaryItemsTable items={report.items ?? []} />
        </CardContent>
      </Card>
    </ReportShell>
  );
}

function ReportShell({
  periodLabel,
  children,
}: {
  periodLabel: string;
  children: ReactNode;
}) {
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Button variant="outline" size="sm" asChild>
          <Link href="/admin/offline/sales">
            <ArrowLeft className="h-4 w-4" />
            목록
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-semibold">{periodLabel}</h1>
          <p className="text-sm text-muted-foreground">
            오프라인 매출 보고서
          </p>
        </div>
      </div>
      {children}
    </div>
  );
}
