const PAYMENT_SOURCE_TYPE_LABELS: Record<string, string> = {
  CARD: "카드",
  CASH: "현금",
  PREPAID_VALUE: "선불지급수단",
  BARCODE: "간편결제",
  ACCOUNT_TRANSFER: "계좌이체",
  GIFT_CARD: "상품권",
  EXTERNAL: "외부결제수단",
  UNDEFINED: "UNDEFINED",
};

export function formatPaymentSourceType(
  sourceType: string | null | undefined,
): string {
  if (!sourceType) {
    return "미지정";
  }
  return PAYMENT_SOURCE_TYPE_LABELS[sourceType] ?? sourceType;
}

export function formatCurrency(value: number | null | undefined): string {
  if (value == null) {
    return "-";
  }
  return `₩${value.toLocaleString()}`;
}
