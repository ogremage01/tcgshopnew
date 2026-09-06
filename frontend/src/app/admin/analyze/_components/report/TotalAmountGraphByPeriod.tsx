"use client";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { AdminDailySalesReportRowDto } from "@/types/order";
import {
  Chart as ChartJS,
  LinearScale,
  CategoryScale,
  BarElement,
  PointElement,
  LineElement,
  Legend,
  Tooltip,
  LineController,
  BarController,
} from "chart.js";
import { Chart } from "react-chartjs-2";

ChartJS.register(
  LinearScale,
  CategoryScale,
  BarElement,
  PointElement,
  LineElement,
  Legend,
  Tooltip,
  LineController,
  BarController,
);

export default function TotalAmountGraphByPeriod({
  report,
}: {
  report: AdminDailySalesReportRowDto[] | null;
}) {
  if (!report || report.length === 0) {
    return null;
  }

  let cumulativeSum = 0;
  const cumulativeAmounts = report.map((row) => {
    cumulativeSum += row.totalAmount;
    return cumulativeSum;
  });

  const data = {
    labels: report.map((row) => row.orderDate),
    datasets: [
      {
        type: "line" as const,
        label: "누적 합계",
        data: cumulativeAmounts,
        backgroundColor: "rgba(255, 205, 86, 0.5)",
        borderColor: "rgb(255, 205, 86)",
        borderWidth: 1,
      },
      {
        type: "bar" as const,
        label: "총계",
        data: report.map((row) => row.totalAmount),
        borderColor: "rgb(54, 162, 235)",
        backgroundColor: "rgba(54, 162, 235, 0.5)",
      },
    ],
  };
  const options = {
    responsive: true,
    plugins: {
      legend: {
        position: "top" as const,
      },
    },
  };
  return (
    <Card>
      <CardHeader>
        <CardTitle>기간별 매출 그래프</CardTitle>
        <CardDescription>
          기간: {report[0].orderDate} ~ {report[report.length - 1].orderDate}{" "}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-row flex-wrap gap-4 h-96 items-center justify-center">
        <Chart type="bar" data={data} options={options} />
      </CardContent>
    </Card>
  );
}
