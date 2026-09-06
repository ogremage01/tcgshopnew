"use client";

import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
} from "@/components/ui/table";
import { OfflineSalesSummaryPaymentDto } from "@/types/sales";
import {
  formatCurrency,
  formatPaymentSourceType,
} from "../_lib/payment-source-type";

interface SalesSummaryPaymentsTableProps {
  payments: OfflineSalesSummaryPaymentDto[];
}

export default function SalesSummaryPaymentsTable({
  payments,
}: SalesSummaryPaymentsTableProps) {
  if (payments.length === 0) {
    return (
      <div className="flex h-16 items-center justify-center text-muted-foreground">
        집계된 결제 수단 데이터가 없습니다.
      </div>
    );
  }

  const totalAmount = payments.reduce(
    (sum, payment) => sum + payment.totalAmount,
    0,
  );

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>결제 수단</TableHead>
          <TableHead className="text-right">건수</TableHead>
          <TableHead className="text-right">결제 금액</TableHead>
          <TableHead className="text-right">세액</TableHead>
          <TableHead className="text-right">공급가액</TableHead>
          <TableHead className="text-right">면세 금액</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {payments.map((payment, index) => (
          <TableRow key={`${payment.sourceType ?? "unknown"}-${index}`}>
            <TableCell>{formatPaymentSourceType(payment.sourceType)}</TableCell>
            <TableCell className="text-right">
              {payment.paymentCount.toLocaleString()}
            </TableCell>
            <TableCell className="text-right">
              {formatCurrency(payment.totalAmount)}
            </TableCell>
            <TableCell className="text-right">
              {formatCurrency(payment.totalTaxAmount)}
            </TableCell>
            <TableCell className="text-right">
              {formatCurrency(payment.totalSupplyAmount)}
            </TableCell>
            <TableCell className="text-right">
              {formatCurrency(payment.totalTaxExemptAmount)}
            </TableCell>
          </TableRow>
        ))}
        <TableRow className="bg-muted/40 font-medium">
          <TableCell>합계</TableCell>
          <TableCell className="text-right">
            {payments
              .reduce((sum, payment) => sum + payment.paymentCount, 0)
              .toLocaleString()}
          </TableCell>
          <TableCell className="text-right">
            {formatCurrency(totalAmount)}
          </TableCell>
          <TableCell className="text-right">
            {formatCurrency(
              payments.reduce((sum, payment) => sum + payment.totalTaxAmount, 0),
            )}
          </TableCell>
          <TableCell className="text-right">
            {formatCurrency(
              payments.reduce(
                (sum, payment) => sum + payment.totalSupplyAmount,
                0,
              ),
            )}
          </TableCell>
          <TableCell className="text-right">
            {formatCurrency(
              payments.reduce(
                (sum, payment) => sum + payment.totalTaxExemptAmount,
                0,
              ),
            )}
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>
  );
}
