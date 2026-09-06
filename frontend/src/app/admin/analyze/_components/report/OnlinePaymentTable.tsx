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

export function OnlinePaymentTable({ report, showSubtotal = false }: Props) {
  return (
    <table className="w-full border-collapse border border-black table-fixed">
      <thead>
        <tr>
          <th className={labelHead}>결제</th>
          <th className={countHead}>주문</th>
          <th className={amountHead}>총 매출액</th>
          <th className={amountHead}>적립금 사용액</th>
          <th className={amountHead}>결제액</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td className={labelCell}>매장</td>
          <td className={countCell}>
            {formatCount(report.totalOnlineStoreOrderCount)}
          </td>
          <td className={amountCell}>
            {formatCurrency(report.totalOnlineStoreOrderAmount)}
          </td>
          <td className={deductionCell}>
            {formatDeduction(report.totalOnlineStoreUsedPoint)}
          </td>
          <td className={amountCell}>
            {formatCurrency(report.totalOnlineStorePaymentAmount)}
          </td>
        </tr>
        <tr>
          <td className={labelCell}>카드</td>
          <td className={countCell}>
            {formatCount(report.totalOnlineCardOrderCount)}
          </td>
          <td className={amountCell}>
            {formatCurrency(report.totalOnlineCardOrderAmount)}
          </td>
          <td className={deductionCell}>
            {formatDeduction(report.totalOnlineCardUsedPoint)}
          </td>
          <td className={amountCell}>
            {formatCurrency(report.totalOnlineCardPaymentAmount)}
          </td>
        </tr>
        <tr>
          <td className={labelCell}>간편결제</td>
          <td className={countCell}>
            {formatCount(report.totalOnlineEasyPayOrderCount)}
          </td>
          <td className={amountCell}>
            {formatCurrency(report.totalOnlineEasyPayOrderAmount)}
          </td>
          <td className={deductionCell}>
            {formatDeduction(report.totalOnlineEasyPayUsedPoint)}
          </td>
          <td className={amountCell}>
            {formatCurrency(report.totalOnlineEasyPayPaymentAmount)}
          </td>
        </tr>
        {showSubtotal && (
          <tr>
            <td className={labelCell}>소계</td>
            <td className={countCell}>
              {formatCount(
                (report.totalOnlineStoreOrderCount ?? 0) +
                  (report.totalOnlineCardOrderCount ?? 0) +
                  (report.totalOnlineEasyPayOrderCount ?? 0),
              )}
            </td>
            <td className={amountCell}>
              {formatCurrency(
                (report.totalOnlineStoreOrderAmount ?? 0) +
                  (report.totalOnlineCardOrderAmount ?? 0) +
                  (report.totalOnlineEasyPayOrderAmount ?? 0),
              )}
            </td>
            <td className={deductionCell}>
              {formatDeduction(
                (report.totalOnlineStoreUsedPoint ?? 0) +
                  (report.totalOnlineCardUsedPoint ?? 0) +
                  (report.totalOnlineEasyPayUsedPoint ?? 0),
              )}
            </td>
            <td className={amountCell}>
              {formatCurrency(
                (report.totalOnlineStorePaymentAmount ?? 0) +
                  (report.totalOnlineCardPaymentAmount ?? 0) +
                  (report.totalOnlineEasyPayPaymentAmount ?? 0),
              )}
            </td>
          </tr>
        )}
      </tbody>
    </table>
  );
}
