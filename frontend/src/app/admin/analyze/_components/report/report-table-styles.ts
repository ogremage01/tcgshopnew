import { formatCurrency } from "@/app/admin/offline/sales/_lib/payment-source-type";

export const cell = "text-right border border-black px-2 whitespace-nowrap";
export const head = "text-center border border-black px-2 whitespace-nowrap";
export const amountCell = `${cell} min-w-[8rem]`;
export const amountHead = `${head} min-w-[8rem]`;
export const labelCell = `${cell} w-16 min-w-16 max-w-16 px-1`;
export const labelHead = `${head} w-16 min-w-16 max-w-16 px-1`;
/** 세부 매출 표: 카테고리/세트명 등 긴 텍스트용 */
export const detailLabelCell =
  "text-left border border-black px-2 whitespace-nowrap w-[45%]";
export const detailLabelHead =
  "text-center border border-black px-2 whitespace-nowrap w-[45%]";
/** 매출 비중(%) 등 짧은 수치용 */
export const pctCell = `${cell} w-16 min-w-16 max-w-16 px-1`;
export const pctHead = `${head} w-16 min-w-16 max-w-16 px-1`;
export const countCell = `${cell} w-12 min-w-12 max-w-12 px-1`;
export const countHead = `${head} w-12 min-w-12 max-w-12 px-1`;
export const deductionCell = `${amountCell} text-red-500`;

export function formatCount(value: number | null | undefined): string {
  if (value == null) return "-";
  return value.toLocaleString();
}

export function formatDeduction(value: number | null | undefined): string {
  if (value == null || value === 0) return "-";
  return `▲${formatCurrency(value)}`;
}

/** 음수 수량을 리포트 차감과 같이 ▲ + 절댓값으로 표시 */
export function formatDeductionCount(value: number | null | undefined): string {
  if (value == null) return "-";
  if (value < 0) return `▲${Math.abs(value).toLocaleString()}`;
  return value.toLocaleString();
}
