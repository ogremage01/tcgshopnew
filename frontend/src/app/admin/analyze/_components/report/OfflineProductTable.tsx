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
  showSingleCard?: boolean;
}

export function OfflineProductTable({ report, showSingleCard = false }: Props) {
  return (
    <table className="w-full border-collapse border border-black table-fixed">
      <thead>
        <tr>
          <th className={labelHead}>종합</th>
          <th className={countHead}>종수</th>
          <th className={countHead}>수량</th>
          <th className={amountHead}>매출액</th>
          <th className={amountHead}>할인액</th>
          <th className={amountHead}>결제액</th>
        </tr>
      </thead>
      <tbody>
        {showSingleCard && (
          <tr>
            <td className={labelCell}>싱글카드</td>
            <td className={countCell}>{formatCount(report.totalOfflineSingleCardOrderCategoryCount)}</td>
            <td className={countCell}>{formatCount(report.totalOfflineSingleCardOrderQuantity)}</td>
            <td className={amountCell}>{formatCurrency(report.totalOfflineSingleCardOrderAmount)}</td>
            <td className={deductionCell}>{formatDeduction(report.totalOfflineSingleCardDiscountAmount)}</td>
            <td className={amountCell}>{formatCurrency(report.totalOfflineSingleCardPaymentAmount)}</td>
          </tr>
        )}
        <tr>
          <td className={labelCell}>밀봉제품</td>
          <td className={countCell}>{formatCount(report.totalOfflineSealedProductOrderCategoryCount)}</td>
          <td className={countCell}>{formatCount(report.totalOfflineSealedProductOrderQuantity)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineSealedProductOrderAmount)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalOfflineSealedProductDiscountAmount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineSealedProductPaymentAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>서플라이</td>
          <td className={countCell}>{formatCount(report.totalOfflineSupplyOrderCount)}</td>
          <td className={countCell}>{formatCount(report.totalOfflineSupplyOrderQuantity)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineSupplyOrderAmount)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalOfflineSupplyDiscountAmount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineSupplyPaymentAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>기타제품</td>
          <td className={countCell}>{formatCount(report.totalOfflineOtherProductCount)}</td>
          <td className={countCell}>{formatCount(report.totalOfflineOtherProductQuantity)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineOtherProductAmount)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalOfflineOtherProductDiscountAmount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineOtherProductPaymentAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>소계</td>
          <td className={countCell}>-</td>
          <td className={countCell}>-</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineTotalAmount)}</td>
          <td className={deductionCell}>{formatDeduction(report.totalOfflineTotalDiscountAmount)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOfflineTotalPaymentAmount)}</td>
        </tr>
      </tbody>
    </table>
  );
}
