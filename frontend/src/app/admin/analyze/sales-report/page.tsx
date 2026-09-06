"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import DailySalesReportListPage from "../_components/DaliyReport";
import WeeklyReport from "../_components/WeeklyReport";
import MonthlyReport from "../_components/MonthlyReport";

const TABS = ["daily", "weekly", "monthly"] as const;
type TabValue = (typeof TABS)[number];

function SalesReportTabs() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const tabParam = searchParams?.get("tab");
  const activeTab: TabValue = TABS.includes(tabParam as TabValue)
    ? (tabParam as TabValue)
    : "daily";

  const handleTabChange = (value: string) => {
    router.replace(`/admin/analyze/sales-report?tab=${value}`);
  };

  return (
    <Tabs value={activeTab} onValueChange={handleTabChange}>
      <TabsList>
        <TabsTrigger value="daily">일간</TabsTrigger>
        <TabsTrigger value="weekly">주간</TabsTrigger>
        <TabsTrigger value="monthly">월간</TabsTrigger>
      </TabsList>
      <TabsContent value="daily">
        <DailySalesReportListPage />
      </TabsContent>
      <TabsContent value="weekly">
        <WeeklyReport />
      </TabsContent>
      <TabsContent value="monthly">
        <MonthlyReport />
      </TabsContent>
    </Tabs>
  );
}

export default function SalesReportPage() {
  return (
    <div>
      <Suspense fallback={null}>
        <SalesReportTabs />
      </Suspense>
    </div>
  );
}
