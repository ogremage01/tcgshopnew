import { AdminDailyReportDto } from "@/types/order";
import { formatCurrency } from "@/app/admin/offline/sales/_lib/payment-source-type";
import {
  amountCell,
  amountHead,
  countCell,
  countHead,
  deductionCell,
  formatCount,
  formatDeduction,
  labelCell,
  labelHead,
} from "./report-table-styles";

interface Props {
  report: AdminDailyReportDto;
}

export function TotalSummaryTable({ report }: Props) {
  return (
    <table className="w-full border-collapse border border-black table-fixed">
      <thead>
        <tr>
          <th className={labelHead}>구분</th>
          <th className={countHead}>주문</th>
          <th className={amountHead}>총 매출액</th>
          <th className={amountHead}>적립금 사용액</th>
          <th className={amountHead}>할인액</th>
          <th className={amountHead}>결제액</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td className={labelCell}>합계</td>
          <td className={countCell}>{formatCount(report.totalOrderCount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOrderAmount)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalUsedPoint)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalDiscountAmount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalPaymentAmount)}</td>
        </tr>
      </tbody>
    </table>
  );
}
