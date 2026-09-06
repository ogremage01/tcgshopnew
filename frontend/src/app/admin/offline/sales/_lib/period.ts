import { OfflineSalesSummaryPeriod } from "@/types/sales";

export const PERIOD_OPTIONS: {
  label: string;
  value: OfflineSalesSummaryPeriod;
}[] = [
  { label: "일별", value: "DAILY" },
  { label: "주별", value: "WEEKLY" },
  { label: "월별", value: "MONTHLY" },
  { label: "분기별", value: "QUARTER" },
  { label: "연도별", value: "YEARLY" },
  { label: "전체", value: "TOTAL" },
];

export function addDays(dateStr: string, days: number): string {
  const [year, month, day] = dateStr.split("-").map(Number);
  const date = new Date(year, month - 1, day + days);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
}

export function formatPeriodLabel(
  period: OfflineSalesSummaryPeriod,
  periodStart: string | null,
): string {
  if (period === "TOTAL" || !periodStart) {
    return "전체";
  }

  const [year, month] = periodStart.split("-").map(Number);

  switch (period) {
    case "DAILY":
      return periodStart;
    case "WEEKLY": {
      const weekEnd = addDays(periodStart, 6);
      return `${periodStart} ~ ${weekEnd}`;
    }
    case "MONTHLY":
      return `${year}-${String(month).padStart(2, "0")}`;
    case "QUARTER":
      return `${year}-Q${Math.ceil(month / 3)}`;
    case "YEARLY":
      return String(year);
    default:
      return periodStart;
  }
}

export function getPeriodOptionLabel(period: OfflineSalesSummaryPeriod): string {
  return PERIOD_OPTIONS.find((option) => option.value === period)?.label ?? period;
}

export function buildReportHref(
  period: OfflineSalesSummaryPeriod,
  periodStart: string | null,
): string {
  const params = new URLSearchParams({ period });
  if (periodStart) {
    params.set("periodStart", periodStart);
  }
  return `/admin/offline/sales/report?${params.toString()}`;
}

export function isOfflineSalesSummaryPeriod(
  value: string | null,
): value is OfflineSalesSummaryPeriod {
  return PERIOD_OPTIONS.some((option) => option.value === value);
}
