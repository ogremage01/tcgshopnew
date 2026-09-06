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
  showSubtotal?: boolean;
}

export function OfflinePaymentTable({ report, showSubtotal = false }: Props) {
  return (
    <table className="w-full border-collapse border border-black table-fixed">
      <thead>
        <tr>
          <th className={labelHead}>결제</th>
          <th className={countHead}>주문</th>
          <th className={amountHead}>총 매출액</th>
          <th className={amountHead}>할인액</th>
          <th className={amountHead}>결제액</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td className={labelCell}>현금</td>
          <td className={countCell}>{formatCount(report.totalOfflineCashCount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineCashAmount)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalOfflineCashDiscountAmount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineCashPaymentAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>카드/간편</td>
          <td className={countCell}>{formatCount(report.totalOfflineCardOrderCount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineCardOrderAmount)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalOfflineCardDiscountAmount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineCardPaymentAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>기타</td>
          <td className={countCell}>{formatCount(report.totalOfflineOtherPaymentCount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineOtherPaymentAmount)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalOfflineOtherPaymentDiscountAmount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineOtherPaymentPaymentAmount)}</td>
        </tr>
        {showSubtotal && (
          <tr>
            <td className={labelCell}>소계</td>
            <td className={countCell}>
              {formatCount(
                (report.totalOfflineCashCount ?? 0) +
                (report.totalOfflineCardOrderCount ?? 0) +
                (report.totalOfflineOtherPaymentCount ?? 0),
              )}
            </td>
            <td className={amountCell}>
              {formatCurrency(
                (report.totalOfflineCashAmount ?? 0) +
                (report.totalOfflineCardOrderAmount ?? 0) +
                (report.totalOfflineOtherPaymentAmount ?? 0),
              )}
            </td>
            <td className={deductionCell}>
              {formatDeduction(
                (report.totalOfflineCashDiscountAmount ?? 0) +
                (report.totalOfflineCardDiscountAmount ?? 0) +
                (report.totalOfflineOtherPaymentDiscountAmount ?? 0),
              )}
            </td>
            <td className={amountCell}>
              {formatCurrency(
                (report.totalOfflineCashPaymentAmount ?? 0) +
                (report.totalOfflineCardPaymentAmount ?? 0) +
                (report.totalOfflineOtherPaymentPaymentAmount ?? 0),
              )}
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
}
