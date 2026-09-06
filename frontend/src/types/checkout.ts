export type DraftStatus = "READY" | "CONFIRMED" | "CANCELLED" | "EXPIRED" | "REPLACED";
export type DeliveryMethod = "DELIVERY" | "STORE_PICKUP";

export interface CheckoutDraftItemResponse {
  searchMapId: number;
  productType: string;
  productNameEn: string;
  productNameKo: string;
  imageUrl: string;
  /** 썸네일: 영문 우선 표시용 */
  imageUrlEn?: string | null;
  imageUrlKo?: string | null;
  snapshotUnitPrice: number;
  quantity: number;
  snapshotTotalPrice: number;
  snapshotPointAmount: number;
}

export interface CheckoutDraftResponse {
  publicId: string;
  status: DraftStatus;
  deliveryMethod: DeliveryMethod | null;
  recipientName: string | null;
  recipientAddress: string | null;
  recipientAddressDetail?: string | null;
  recipientPostalCode?: string | null;
  recipientPhone: string | null;
  recipientEmail: string | null;
  orderRequest: string | null;
  subtotalAmount: number;
  deliveryFee: number;
  /** 무료배송 적용 전 기준 배송비(택배 배송 시) */
  standardDeliveryFee?: number | null;
  /** 무료배송 기준 금액. 비활성이면 null */
  freeShippingThreshold?: number | null;
  usedPointAmount: number;
  totalAmount: number;
  /** 상품 합계(배송비 제외). 서버가 내려주지 않으면 `subtotalAmount`와 동일하게 취급 가능 */
  totalProductAmount?: number | null;
  expiresAt: string;
  createdAt: string;
  confirmedOrderId: number | null;
  items: CheckoutDraftItemResponse[];
  hasEventTicket: boolean;
}

export interface PatchCheckoutDraftRequest {
  deliveryMethod?: DeliveryMethod;
  recipientName?: string;
  recipientAddress?: string;
  recipientAddressDetail?: string;
  recipientPostalCode?: string;
  recipientPhone?: string;
  recipientEmail?: string;
  orderRequest?: string;
  usedPointAmount?: number;
  /** 결제 통화. 현재 KRW 고정 */
  paymentCurrency?: string;
  /** 결제 통화 환율. 현재 미사용(KRW 1:1) */
  paymentCurrencyRate?: number;
  /** 정산 원화 금액 */
  settleKrwAmount?: number;
}

export interface CheckoutConfirmResponse {
  orderId: number;
  paymentStatus: string;
  confirmedAt: string;
  guestVerificationCode?: string | null;
}

export interface TossPaymentConfirmRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export interface CheckoutConfirmFailedItem {
  searchMapId: number;
  productNameKo: string;
  reason: string;
  snapshotUnitPrice: number;
  currentUnitPrice: number | null;
  requestedQuantity: number;
  availableStock: number;
}

export interface CheckoutConfirmFailureResponse {
  code: string;
  message: string;
  failedItems: CheckoutConfirmFailedItem[];
}

export interface CreateDraftResponse {
  publicId: string;
}
