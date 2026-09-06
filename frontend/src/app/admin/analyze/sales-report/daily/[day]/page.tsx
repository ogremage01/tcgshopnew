"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { api } from "@/lib/api";
import { AdminDailyReportDto } from "@/types/order";
import { ReportPageShell } from "@/app/admin/analyze/_components/report/ReportPageShell";
import { TotalSummaryTable } from "@/app/admin/analyze/_components/report/TotalSummaryTable";
import { OnlinePaymentTable } from "@/app/admin/analyze/_components/report/OnlinePaymentTable";
import { OnlineProductTable } from "@/app/admin/analyze/_components/report/OnlineProductTable";
import { OfflinePaymentTable } from "@/app/admin/analyze/_components/report/OfflinePaymentTable";
import { OfflineProductTable } from "@/app/admin/analyze/_components/report/OfflineProductTable";

export default function DailySalesReportPage() {
  const params = useParams<{ day: string }>();
  const day = params?.day;
  const [report, setReport] = useState<AdminDailyReportDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!day) return;

    const fetchReport = async () => {
      try {
        const response = await api.get<AdminDailyReportDto>(
          `/api/admin/analyze/daily-sales-report/${day}`,
        );
        setReport(response);
        setError(null);
      } catch {
        setError("일간 매출 보고를 불러오지 못했습니다.");
        setReport(null);
      }
    };

    fetchReport();
  }, [day]);

  return (
    <ReportPageShell
      title={`일간 매출 세부 보고(${day})`}
      error={error}
      loaded={report != null}
      backTab="daily"
    >
      {report && (
        <>
          <div className="flex flex-row gap-4">
            <Card className="w-1/4">
              <CardHeader className="text-lg font-bold">기간: {day}</CardHeader>
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
          <div className="flex flex-row gap-4">
            <Card className="w-1/2">
              <CardHeader>
                <CardTitle className="text-center">온라인 매출</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col text-xs gap-4">
                <OnlinePaymentTable report={report} showSubtotal />
                <Separator />
                <OnlineProductTable report={report} />
              </CardContent>
            </Card>
            <Card className="w-1/2">
              <CardHeader>
                <CardTitle className="text-center">오프라인 매출</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col text-xs gap-4">
                <OfflinePaymentTable report={report} showSubtotal />
                <Separator />
                <OfflineProductTable report={report} showSingleCard />
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </ReportPageShell>
  );
}
