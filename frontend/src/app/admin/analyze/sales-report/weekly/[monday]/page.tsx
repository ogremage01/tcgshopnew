"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { api } from "@/lib/api";
import { AdminWeeklyReportDto } from "@/types/order";
import { ReportPageShell } from "@/app/admin/analyze/_components/report/ReportPageShell";
import { TotalSummaryTable } from "@/app/admin/analyze/_components/report/TotalSummaryTable";
import { OnlinePaymentTable } from "@/app/admin/analyze/_components/report/OnlinePaymentTable";
import { OnlineProductTable } from "@/app/admin/analyze/_components/report/OnlineProductTable";
import { OfflinePaymentTable } from "@/app/admin/analyze/_components/report/OfflinePaymentTable";
import { OfflineProductTable } from "@/app/admin/analyze/_components/report/OfflineProductTable";
import { WeeklyOnlineDetailSection } from "@/app/admin/analyze/_components/report/WeeklyOnlineDetailSection";
import { WeeklyOfflineDetailSection } from "@/app/admin/analyze/_components/report/WeeklyOfflineDetailSection";
import TotalAmountGraphByPeriod from "@/app/admin/analyze/_components/report/TotalAmountGraphByPeriod";
function formatWeekLabel(monday: string): string {
  const start = new Date(monday);
  const end = new Date(monday);
  end.setDate(end.getDate() + 6);
  const fmt = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  return `${fmt(start)} ~ ${fmt(end)}`;
}

export default function WeeklySalesReportPage() {
  const params = useParams<{ monday: string }>();
  const monday = params?.monday ?? "";
  const [report, setReport] = useState<AdminWeeklyReportDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!monday) return;

    const fetchReport = async () => {
      try {
        const response = await api.get<AdminWeeklyReportDto>(
          `/api/admin/analyze/weekly-sales-report/${monday}`,
        );
        setReport(response);
        setError(null);
      } catch {
        setError("주간 매출 보고를 불러오지 못했습니다.");
        setReport(null);
      }
    };

    fetchReport();
  }, [monday]);

  const weekLabel = formatWeekLabel(monday);

  return (
    <ReportPageShell
      title={`주간 매출 세부 보고(${weekLabel})`}
      error={error}
      loaded={report != null}
      backTab="weekly"
    >
      {report && (
        <>
          <div className="flex flex-row gap-4">
            <Card className="w-1/4">
              <CardHeader className="text-lg font-bold">{weekLabel}</CardHeader>
            </Card>
            <Card className="w-3/4">
              <CardHeader>
                <CardTitle className="text-center">
                  총계(온라인 + 오프라인-직접 결제)
                </CardTitle>
              </CardHeader>
              <CardContent>
                <TotalSummaryTable report={report} />
              </CardContent>
            </Card>
          </div>
          <TotalAmountGraphByPeriod
            report={report?.dailySalesReportRowDtoList}
          />
          <div className="flex flex-row gap-4">
            <Card className="w-1/2">
              <CardHeader>
                <CardTitle className="text-center">온라인 매출</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col text-xs gap-4">
                <OnlinePaymentTable report={report} />
                <Separator />
                <OnlineProductTable report={report} />
                <WeeklyOnlineDetailSection report={report} />
              </CardContent>
            </Card>
            <Card className="w-1/2">
              <CardHeader>
                <CardTitle className="text-center">오프라인 매출</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col text-xs gap-4">
                <OfflinePaymentTable report={report} />
                <Separator />
                <OfflineProductTable report={report} showSingleCard />
                <Separator />
                <WeeklyOfflineDetailSection report={report} />
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </ReportPageShell>
  );
}
