import { PageParam } from "./pagination";

export interface OrderSimpleDto {
  id: number;
  orderDate: Date;
  orderTotal: number;
  orderStatus: string;
  orderAmount: number;
  paymentCurrency: string;
}

/** 백엔드 `com.shop.order.dto.OrderProductDto` (BigDecimal → JSON number). */
export interface OrderProductDto {
  id: number;
  orderInfoId: number;
  productId: number;
  quantity: number;
  price: number;
  totalPrice: number;
  imageUrl: string | null;
  productNameEn: string | null;
  productNameKo: string | null;
  rewardPoints: number | null;
}

/** 마이페이지 등 레거시 UI용(백엔드 `OrderProductDto`와 다름). */
export interface MypageOrderProductLineDto {
  id: number;
  name: string;
  price: number;
  quantity: number;
  total: number;
  imageUrl: string;
}

export interface OrderDetailDto {
  id: number;
  orderDate: string;
  orderTotal: number;
  orderStatus: string;
  orderAmount: number;
  paymentCurrency: string;
  paymentStatus: string;
  deliveryCompany: string;
  deliveryTrackingNumber: string;
  deliveryMemo: string;
  usedPointAmount: number;
  actualPaymentAmount: number;
  paymentDate: string;
  orderProducts: MypageOrderProductLineDto[];
}

export interface OrderConfigDto {
  configKey: string;
  configValue: string;
  isEnabled: boolean;
}

/** 백엔드 `com.shop.order.dto.AdminOrderSimpleDto` (`orderDate`는 LocalDateTime → ISO 문자열). */
export interface AdminOrderSimpleDto {
  id: number;
  orderDate: string;
  customerName: string;
  customerContact: string;
  customerEmail: string;
  orderStatus: string;
  deliveryCompany: string | null;
  totalPaymentAmount: number;
  /** order_products.quantity 합계(총 개) */
  totalQuantity: number;
  /** 주문 라인 수(몇 종) */
  orderLineCount: number;
  usedPointAmount: number;
  deliveryFee: number;
  actualPaymentAmount: number;
  paymentMethod: string | null;
}

/** 백엔드 `com.shop.admin.order.dto.AdminTodayOrderSummaryDto` */
export interface AdminTodayOrderSummaryDto {
  totalOrderCount: number;
  totalOrderAmount: number;
  orderPendingCount: number;
  orderCompletedCount: number;
  orderReceivedCount: number;
}

/** 백엔드 `com.shop.admin.order.dto.AdminTotalOrderSummaryDto` */
export interface AdminTotalOrderSummaryDto {
  orderPendingCount: number;
  orderCompletedCount: number;
  orderReceivedCount: number;
}
export interface AdminSimpleOrderRequest {
  pageParam: PageParam;
  /**
   * 미선택 시 null.
   * `Date`를 그대로 JSON.stringify 하면 `...Z`(UTC)가 되어 서버 JVM 타임존에 따라 LocalDateTime이 어긋날 수 있음.
   * `"2026-05-01T00:00:00.000"`처럼 타임존 없는 로컬 시각 문자열로 보냄.
   */
  startDate: string | null;
  endDate: string | null;
  orderStatusList: string[];
}

/** 백엔드 `com.shop.admin.order.dto.AdminOrderSearchRequest` */
export interface AdminOrderSearchRequest {
  pageParam: PageParam;
  keyword: string;
}

/** 백엔드 `com.shop.order.dto.OrderInfoDto` */
export interface OrderInfoDto {
  id: number;
  guest: boolean | null;
  userMemo: string | null;
  userId: number | null;
  recipientName: string | null;
  recipientAddress: string | null;
  /** 상세주소 */
  recipientAddressDetail: string | null;
  recipientPhone: string | null;
  recipientEmail: string | null;
  orderRequest: string | null;
  orderStatus: string | null;
  paymentStatus: string | null;
  deliveryCompany: string | null;
  deliveryTrackingNumber: string | null;
  deliveryMemo: string | null;
  paymentCurrency: string | null;
  /** 상품 합계(배송비 제외). 구 주문은 null일 수 있음. */
  totalProductAmount: number | null;
  totalPaymentAmount: number;
  usedPointAmount: number;
  actualPaymentAmount: number;
  /** LocalDateTime → ISO 문자열 */
  paymentDate: string | null;
  deliveryFee: number;
  totalQuantity: number;
  orderLineCount: number;
  paymentMethod: string | null;
  /** 토스 paymentKey 등 PG 거래 ID */
  pgTransactionId?: string | null;
  paymentCurrencyRateSnapshot: number;
  /** 주문 내 적립금 합계 (주문 시점 계산) */
  totalEarnedPoints: number | null;
  /** 배송 주소 우편번호 */
  postalCode: string | null;
}

/** 백엔드 `com.shop.order.dto.OrderDto` */
export interface OrderDto {
  orderInfo: OrderInfoDto;
  orderProducts: OrderProductDto[];
}

/** 백엔드 `com.shop.admin.order.dto.AdminOrderCardProductGroupDto` */
export interface AdminOrderCardProductGroupDto {
  game: string;
  products: AdminOrderCardProductDto[];
}

/** 백엔드 `com.shop.admin.order.dto.AdminOrderDetailDto` */
export interface AdminOrderDetailDto {
  orderInfo: OrderInfoDto;
  orderCardProductGroups: AdminOrderCardProductGroupDto[];
  orderManualProducts: AdminOrderManualProductDto[];
  orderSealedProducts: AdminOrderSealedProductDto[];
  orderSupplyProducts: AdminOrderSupplyProductDto[];
}

/** 백엔드 `com.shop.order.dto.UserOrderCardProductGroupDto` */
export interface UserOrderCardProductGroupDto {
  game: string;
  products: UserOrderCardProductDto[];
}

/** 백엔드 `com.shop.order.dto.UserOrderDetailDto` */
export interface UserOrderDetailDto {
  orderInfo: OrderInfoDto;
  orderCardProductGroups: UserOrderCardProductGroupDto[];
  orderManualProducts: UserOrderManualProductDto[];
  orderSealedProducts: UserOrderSealedProductDto[];
  orderSupplyProducts: UserOrderSupplyProductDto[];
}

/** 백엔드 `com.shop.order.dto.UserOrderCardProductDto` */
export interface UserOrderCardProductDto extends OrderProductDto {
  game: string | null;
  setCode: string | null;
  setName: string | null;
  releaseDate: string | null;
  setNumber: number | null;
  printType: string | null;
  printing: string | null;
  language: string | null;
  condition: string | null;
}

/** 백엔드 `com.shop.order.dto.UserOrderManualProductDto` */
export type UserOrderManualProductDto = OrderProductDto;

/** 백엔드 `com.shop.order.dto.UserOrderSealedProductDto` */
export interface UserOrderSealedProductDto extends OrderProductDto {
  game: string | null;
  language: string | null;
}

/** 백엔드 `com.shop.order.dto.UserOrderSupplyProductDto` */
export interface UserOrderSupplyProductDto extends OrderProductDto {
  supplyType: string | null;
  maker: string | null;
}

/** 백엔드 `com.shop.admin.order.dto.AdminOrderCardProductDto` */
export interface AdminOrderCardProductDto extends OrderProductDto {
  game: string | null;
  setCode: string | null;
  /** LocalDateTime → ISO 문자열 */
  releaseDate: string | null;
  setNumber: number | null;
  totalStock: number | null;
  memo: string | null;
  storageName: string | null;
  printType: string | null;
  printing: string | null;
  language: string | null;
  condition: string | null;
  setName: string | null;
}

/** 백엔드 `com.shop.admin.order.dto.AdminOrderManualProductDto` (OrderProductDto만 상속) */
export type AdminOrderManualProductDto = OrderProductDto;

/** 백엔드 `com.shop.admin.order.dto.AdminOrderSealedProductDto` */
export interface AdminOrderSealedProductDto extends OrderProductDto {
  game: string | null;
  language: string | null;
  totalStock: number | null;
}

/** 백엔드 `com.shop.admin.order.dto.AdminOrderSupplyProductDto` */
export interface AdminOrderSupplyProductDto extends OrderProductDto {
  supplyType: string | null;
  maker: string | null;
  totalStock: number | null;
}

export interface OrderDailySalesSummaryDto {
  /** LocalDateTime → ISO 문자열 */
  orderDate: string;
  totalOrderCount: number;
  totalOrderAmount: number;
  totalUsedPointAmount: number;
  totalDeliveryFee: number;
  totalActualPaymentAmount: number;
}
export interface AdminSalesSummarySimpleDto {
  orderMonth: number;
  totalOrderCount: number;
  totalOrderAmount: number;
  totalUsedPointAmount: number;
  totalDeliveryFee: number;
  totalActualPaymentAmount: number;
}
export interface AdminStockSummaryDto {
  gameName: string;
  totalStockCount: number;
  totalStockAmount: number;
}
export interface AdminDailySalesSummarySimpleDto {
  orderDate: string;
  totalOrderCount: number;
  totalOrderAmount: number;
  totalUsedPointAmount: number;
  totalDeliveryFee: number;
  totalActualPaymentAmount: number;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminDailySalesReportRowDto` */
export interface AdminDailySalesReportRowDto {
  /** LocalDate → "YYYY-MM-DD" 문자열 */
  orderDate: string;
  totalAmount: number;
  onlineAmount: number;
  offlineAmount: number;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklySalesReportRowDto` */
export interface AdminWeeklySalesReportRowDto {
  /** 해당 주의 월요일 LocalDate → "YYYY-MM-DD" 문자열 */
  monday: string;
  totalAmount: number;
  onlineAmount: number;
  offlineAmount: number;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminMonthlySalesReportRowDto` */
export interface AdminMonthlySalesReportRowDto {
  /** 해당 월의 1일 LocalDate → "YYYY-MM-DD" 문자열 */
  month: string;
  totalAmount: number;
  onlineAmount: number;
  offlineAmount: number;
}

export interface AdminDailyReportDto {
  totalOrderCount: number | null;
  totalOrderAmount: number | null;
  totalUsedPoint: number | null;
  totalDiscountAmount: number | null;
  totalPaymentAmount: number | null;
  totalOnlineStoreOrderCount: number | null;
  totalOnlineStoreOrderAmount: number | null;
  totalOnlineStoreUsedPoint: number | null;
  totalOnlineStorePaymentAmount: number | null;
  totalOnlineCardOrderCount: number | null;
  totalOnlineCardOrderAmount: number | null;
  totalOnlineCardUsedPoint: number | null;
  totalOnlineCardPaymentAmount: number | null;
  totalOnlineEasyPayOrderCount: number | null;
  totalOnlineEasyPayOrderAmount: number | null;
  totalOnlineEasyPayUsedPoint: number | null;
  totalOnlineEasyPayPaymentAmount: number | null;
  totalOnlineSingleCardOrderCategoryCount: number | null;
  totalOnlineSingleCardOrderQuantity: number | null;
  totalOnlineSingleCardOrderAmount: number | null;
  totalOnlineSealedProductOrderCategoryCount: number | null;
  totalOnlineSealedProductOrderQuantity: number | null;
  totalOnlineSealedProductOrderAmount: number | null;
  totalOnlineSupplyOrderCount: number | null;
  totalOnlineSupplyOrderAmount: number | null;
  totalOnlineSupplyOrderQuantity: number | null;
  totalOnlineOtherProductOrderCategoryCount: number | null;
  totalOnlineOtherProductOrderQuantity: number | null;
  totalOnlineOtherProductAmount: number | null;
  totalOnlineTotalAmount: number | null;
  totalOfflineCashCount: number | null;
  totalOfflineCashAmount: number | null;
  totalOfflineCashDiscountAmount: number | null;
  totalOfflineCashPaymentAmount: number | null;
  totalOfflineCardOrderCount: number | null;
  totalOfflineCardOrderAmount: number | null;
  totalOfflineCardDiscountAmount: number | null;
  totalOfflineCardPaymentAmount: number | null;
  totalOfflineOtherPaymentCount: number | null;
  totalOfflineOtherPaymentAmount: number | null;
  totalOfflineOtherPaymentDiscountAmount: number | null;
  totalOfflineOtherPaymentPaymentAmount: number | null;
  totalOfflineSingleCardOrderCategoryCount: number | null;
  totalOfflineSingleCardOrderQuantity: number | null;
  totalOfflineSingleCardOrderAmount: number | null;
  totalOfflineSingleCardDiscountAmount: number | null;
  totalOfflineSingleCardPaymentAmount: number | null;
  totalOfflineSealedProductOrderCategoryCount: number | null;
  totalOfflineSealedProductOrderQuantity: number | null;
  totalOfflineSealedProductOrderAmount: number | null;
  totalOfflineSealedProductDiscountAmount: number | null;
  totalOfflineSealedProductPaymentAmount: number | null;
  totalOfflineSupplyOrderCount: number | null;
  totalOfflineSupplyOrderAmount: number | null;
  totalOfflineSupplyOrderQuantity: number | null;
  totalOfflineSupplyDiscountAmount: number | null;
  totalOfflineSupplyPaymentAmount: number | null;
  totalOfflineOtherProductCount: number | null;
  totalOfflineOtherProductAmount: number | null;
  totalOfflineOtherProductQuantity: number | null;
  totalOfflineOtherProductDiscountAmount: number | null;
  totalOfflineOtherProductPaymentAmount: number | null;
  totalOfflineTotalAmount: number | null;
  totalOfflineTotalDiscountAmount: number | null;
  totalOfflineTotalPaymentAmount: number | null;
  totalOnlineDeliveryCount: number | null;
  totalOnlineDeliveryFee: number | null;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.CardProductSaleDto` */
export interface AdminCardProductSaleDto {
  setName: string;
  totalSalesCount: number | null;
  totalSalesQuantity: number | null;
  totalSalesAmount: number | null;
  totalSalesPercentage: number | null;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.SingleCardSalesSummaryDto` */
export interface AdminSingleCardSalesSummaryDto {
  game: string;
  cardProductSaleDtoList: AdminCardProductSaleDto[];
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.SealedProductSaleDto` */
export interface AdminSealedProductSaleDto {
  setName: string;
  totalSalesCount: number | null;
  totalSalesQuantity: number | null;
  totalSalesAmount: number | null;
  totalSalesPercentage: number | null;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.SealedProductSalesSummaryDto` */
export interface AdminSealedProductSalesSummaryDto {
  game: string;
  sealedProductSaleDtoList: AdminSealedProductSaleDto[];
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.SupplyProductSaleDto` */
export interface AdminSupplyProductSaleDto {
  supplyType: string;
  totalSalesCount: number | null;
  totalSalesQuantity: number | null;
  totalSalesAmount: number | null;
  totalSalesPercentage: number | null;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.SupplyProductSalesSummaryDto` */
export interface AdminSupplyProductSalesSummaryDto {
  supplyProductSaleDtoList: AdminSupplyProductSaleDto[];
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.ManualProductSaleDto` */
export interface AdminManualProductSaleDto {
  manualType: string;
  totalSalesCount: number | null;
  totalSalesQuantity: number | null;
  totalSalesAmount: number | null;
  totalSalesPercentage: number | null;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.ManualProductSalesSummaryDto` */
export interface AdminManualProductSalesSummaryDto {
  manualProductSaleDtoList: AdminManualProductSaleDto[];
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.OnlineSalesSummaryDto` */
export interface AdminOnlineSalesSummaryDto {
  singleCardSalesSummaryList: AdminSingleCardSalesSummaryDto[] | null;
  sealedProductSalesSummaryList: AdminSealedProductSalesSummaryDto[] | null;
  supplyProductSalesSummaryDto: AdminSupplyProductSalesSummaryDto | null;
  manualProductSalesSummaryDto: AdminManualProductSalesSummaryDto | null;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto.OfflineSalesSummaryDto` */
export interface AdminOfflineSalesSummaryDto {
  /** 밀봉 제품(온라인과 동일하게 게임별 분할) */
  sealedProductSalesSummaryList: AdminSealedProductSalesSummaryDto[] | null;
  supplyProductSaleDtoList: AdminSupplyProductSaleDto[] | null;
  manualProductSaleDtoList: AdminManualProductSaleDto[] | null;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminWeeklyReportDto` (AdminDailyReportDto 확장) */
export interface AdminWeeklyReportDto extends AdminDailyReportDto {
  onlineSalesSummaryDto: AdminOnlineSalesSummaryDto | null;
  offlineSalesSummaryDto: AdminOfflineSalesSummaryDto | null;
  /** 기간별 매출(주간: 일별, 월간: 해당 월 일별) */
  dailySalesReportRowDtoList: AdminDailySalesReportRowDto[] | null;
}

/** 백엔드 `com.shop.admin.analyze.dto.AdminMonthlySalesReportDto` (AdminWeeklyReportDto 확장, 동일 구조) */
export type AdminMonthlySalesReportDto = AdminWeeklyReportDto;

export interface AdminOrderProductModifyItem {
  orderProductId: number;
  newQuantity?: number;
  deleted: boolean;
  restoreStock: boolean;
}

export interface AdminOrderProductModifyRequest {
  items: AdminOrderProductModifyItem[];
  /** Toss 환불 사유. 필수. */
  cancelReason: string;
}

export interface AdminOrderAdjustmentResponse {
  pgRefundSuccess: boolean;
  pgRefundMessage: string | null;
  orderCancelled: boolean;
  cartRestoreApplied: boolean;
  cartLinesFullyRestored: number;
  cartLinesPartiallyRestored: number;
  cartLinesSkipped: number;
}

export type AdminOrderProductModifyResponse = AdminOrderAdjustmentResponse;

export interface AdminOfflineSalesTotalDto {
  category: string;
  item: string;
  quantity: number;
  amount: number;
}
