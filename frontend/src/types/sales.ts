export interface OfflineSalesAppliedDiscountDto {
  title: string | null;
  amount: number | null;
}

export interface OfflineSalesPaymentDto {
  sourceType: string | null;
  amount: number | null;
  taxAmount: number | null;
  supplyAmount: number | null;
  taxExemptAmount: number | null;
}

export interface OfflineSalesInfoDto {
  id: number;
  orderId: string;
  orderState: string;
  orderNumber: string;
  createdAt: string;
  lineItems: OfflineSalesItemDto[];
  payments: OfflineSalesPaymentDto[];
  listPrice: number;
  discountAmount: number;
  taxAmount: number;
  supplyAmount: number;
  taxExemptAmount: number;
  totalAmount: number;
}

export interface OfflineSalesItemDto {
  id: number;
  orderId: string;
  title: string;
  category: string | null;
  priceUnit: number;
  priceValue: number;
  quantity: number;
  memo: string;
  appliedDiscounts: OfflineSalesAppliedDiscountDto[];
}

export type OfflineSalesSummaryPeriod =
  | "DAILY"
  | "WEEKLY"
  | "MONTHLY"
  | "QUARTER"
  | "YEARLY"
  | "TOTAL";

export interface OfflineSalesSummaryItemDto {
  category: string | null;
  title: string;
  totalQuantity: number;
  totalPriceValue: number;
}

export interface OfflineSalesSummaryPaymentDto {
  sourceType: string | null;
  paymentCount: number;
  totalAmount: number;
  totalTaxAmount: number;
  totalSupplyAmount: number;
  totalTaxExemptAmount: number;
}

export interface OfflineSalesSummaryDto {
  periodStart: string | null;
  totalOrderCount: number;
  totalOrderAmount: number;
  totalDiscountAmount: number;
  totalTaxAmount: number;
  totalSupplyAmount: number;
  totalTaxExemptAmount: number;
  totalTotalAmount: number;
  items: OfflineSalesSummaryItemDto[];
  payments: OfflineSalesSummaryPaymentDto[];
}
