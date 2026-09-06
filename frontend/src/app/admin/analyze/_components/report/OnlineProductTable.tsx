import { AdminDailyReportDto } from "@/types/order";
import { formatCurrency } from "@/app/admin/offline/sales/_lib/payment-source-type";
import {
  amountCell,
  amountHead,
  countCell,
  countHead,
  formatCount,
  labelCell,
  labelHead,
} from "./report-table-styles";

interface Props {
  report: AdminDailyReportDto;
}

export function OnlineProductTable({ report }: Props) {
  return (
    <table className="w-full border-collapse border border-black table-fixed">
      <thead>
        <tr>
          <th className={labelHead}>종합</th>
          <th className={countHead}>종수</th>
          <th className={countHead}>수량</th>
          <th className={amountHead}>매출액</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td className={labelCell}>싱글카드</td>
          <td className={countCell}>{formatCount(report.totalOnlineSingleCardOrderCategoryCount)}</td>
          <td className={countCell}>{formatCount(report.totalOnlineSingleCardOrderQuantity)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOnlineSingleCardOrderAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>밀봉제품</td>
          <td className={countCell}>{formatCount(report.totalOnlineSealedProductOrderCategoryCount)}</td>
          <td className={countCell}>{formatCount(report.totalOnlineSealedProductOrderQuantity)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOnlineSealedProductOrderAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>서플라이</td>
          <td className={countCell}>{formatCount(report.totalOnlineSupplyOrderCount)}</td>
          <td className={countCell}>{formatCount(report.totalOnlineSupplyOrderQuantity)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOnlineSupplyOrderAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>기타제품</td>
          <td className={countCell}>{formatCount(report.totalOnlineOtherProductOrderCategoryCount)}</td>
          <td className={countCell}>{formatCount(report.totalOnlineOtherProductOrderQuantity)}</td>
          <td className={amountCell}>{formatCurrency(report.totalOnlineOtherProductAmount)}</td>
        </tr>
        <tr>
          <td className={labelCell}>배송료</td>
          <td className={countCell}>{formatCount(report.totalOnlineDeliveryCount)}</td>
          <td className={countCell}>-</td>
          <td className={amountCell}>{formatCurrency(report.totalOnlineDeliveryFee)}</td>
        </tr>
        <tr>
          <td className={labelCell}>소계</td>
          <td className={countCell}>-</td>
          <td className={countCell}>-</td>
          <td className={amountCell}>{formatCurrency(report.totalOnlineTotalAmount)}</td>
        </tr>
      </tbody>
    </table>
  );
}
